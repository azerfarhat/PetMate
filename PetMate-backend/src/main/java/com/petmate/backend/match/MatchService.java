package com.petmate.backend.match;

import com.petmate.backend.config.AppProperties;
import com.petmate.backend.entity.Conversation;
import com.petmate.backend.entity.Match;
import com.petmate.backend.entity.Notification;
import com.petmate.backend.entity.Pet;
import com.petmate.backend.entity.PetPhoto;
import com.petmate.backend.entity.Swipe;
import com.petmate.backend.entity.User;
import com.petmate.backend.enums.MatchStatus;
import com.petmate.backend.enums.NotificationType;
import com.petmate.backend.enums.SwipeType;
import com.petmate.backend.exception.MatchNotFoundException;
import com.petmate.backend.exception.PetNotFoundException;
import com.petmate.backend.exception.RegistrationException;
import com.petmate.backend.exception.UserNotFoundException;
import com.petmate.backend.match.dto.MatchResponse;
import com.petmate.backend.match.dto.SwipeCandidateResponse;
import com.petmate.backend.match.dto.SwipeRequest;
import com.petmate.backend.match.dto.SwipeResponse;
import com.petmate.backend.repository.BlockRepository;
import com.petmate.backend.repository.ConversationRepository;
import com.petmate.backend.repository.MatchRepository;
import com.petmate.backend.repository.MessageRepository;
import com.petmate.backend.repository.NotificationRepository;
import com.petmate.backend.repository.PetPhotoRepository;
import com.petmate.backend.repository.PetRepository;
import com.petmate.backend.repository.SwipeRepository;
import com.petmate.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Matching pet-to-pet : chaque Pet est un candidat individuel, et un match ne
 * verrouille jamais un couple d'owners. Deux Pet des mêmes owners peuvent donc
 * matcher alors qu'une autre paire est déjà active.
 *
 * Un swipe (LIKE/PASS) est enregistré puis réutilisable : re-swipée, une Pet
 * met simplement son type à jour (le processus repart de zéro) et réapparaît
 * dans les feeds. Un LIKE croisé forme un Match sur la paire de Pet
 * concernée ; une conversation est ouverte et les deux owners notifiés.
 */
@Service
public class MatchService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final PetPhotoRepository petPhotoRepository;
    private final SwipeRepository swipeRepository;
    private final MatchRepository matchRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final BlockRepository blockRepository;
    private final AppProperties appProperties;

    public MatchService(UserRepository userRepository,
                        PetRepository petRepository,
                        PetPhotoRepository petPhotoRepository,
                        SwipeRepository swipeRepository,
                        MatchRepository matchRepository,
                        ConversationRepository conversationRepository,
                        MessageRepository messageRepository,
                        NotificationRepository notificationRepository,
                        BlockRepository blockRepository,
                        AppProperties appProperties) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.petPhotoRepository = petPhotoRepository;
        this.swipeRepository = swipeRepository;
        this.matchRepository = matchRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
        this.blockRepository = blockRepository;
        this.appProperties = appProperties;
    }

    /**
     * Feed de découverte : toutes les Pet actives des owners actifs non
     * bloqués, paginées. Aucun dédoublonnage : chaque Pet est un candidat
     * individuel et réapparaît (pas d'épuisement au swipe).
     */
    @Transactional(readOnly = true)
    public List<SwipeCandidateResponse> candidates(Long userId, int limit, int offset) {
        User me = requireUser(userId);
        PageRequest pageable = PageRequest.of(Math.max(0, offset) / Math.max(1, limit),
                Math.min(Math.max(1, limit), 50));
        List<Pet> pets = petRepository.findCandidates(userId, cooldownCutoff(), pageable).getContent();
        if (pets.isEmpty()) {
            return List.of();
        }
        Map<Long, List<PetPhoto>> photosByPetId = photoIndex(pets);
        return pets.stream()
                .filter(pet -> withinSearchRadius(me, pet))
                .map(pet -> toCandidate(pet, photosByPetId))
                .toList();
    }

    /**
     * Enregistre (ou met à jour) le swipe. En cas de LIKE, vérifie la
     * réciprocité : chaque Pet de l'utilisateur déjà likée par le owner cible
     * forme un match sur sa propre paire de Pet.
     */
    @Transactional
    public SwipeResponse swipe(Long userId, SwipeRequest request) {
        User me = requireUser(userId);
        if (!petRepository.existsByOwnerIdAndActiveTrue(userId)) {
            throw new RegistrationException("Ajoutez un pet actif pour swiper");
        }
        Pet targetPet = petRepository.findByIdWithPhotos(request.targetPetId())
                .orElseThrow(() -> new PetNotFoundException("Pet introuvable"));
        User targetOwner = targetPet.getOwner();

        if (!targetPet.isActive() || !targetOwner.isActive()
                || targetOwner.getId().equals(userId)) {
            throw new PetNotFoundException("Pet introuvable");
        }
        if (blockRepository.existBetweenOwners(userId, targetOwner.getId())) {
            throw new PetNotFoundException("Pet introuvable");
        }

        upsertSwipe(me, targetPet, request.type());

        if (request.type() == SwipeType.LIKE) {
            List<Swipe> reciprocalLikes = swipeRepository
                    .findLikesOnPetsOfUser(targetOwner.getId(), me.getId(), SwipeType.LIKE);
            if (!reciprocalLikes.isEmpty()) {
                SwipeResponse formed = null;
                for (Swipe like : reciprocalLikes) {
                    formed = handleMatch(me, targetOwner, like.getPet(), targetPet);
                }
                return formed;
            }
        }
        return SwipeResponse.notMatched(request.type());
    }

    /**
     * Liste des matchs actifs de l'utilisateur avec les deux Pet et la conversation.
     */
    @Transactional(readOnly = true)
    public List<MatchResponse> myMatches(Long userId) {
        return matchRepository.findForUser(userId).stream()
                .filter(match -> match.getStatus() == MatchStatus.MATCHED)
                .map(this::toMatchResponse)
                .toList();
    }

    /**
     * Un participant supprime le match et sa conversation (messages compris).
     * Le match passe au statut UNMATCHED. Idempotent.
     */
    @Transactional
    public void unmatch(Long userId, Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match introuvable"));
        boolean participant = match.getUser1().getId().equals(userId)
                || match.getUser2().getId().equals(userId);
        if (!participant) {
            throw new MatchNotFoundException("Match introuvable");
        }
        if (match.getStatus() == MatchStatus.UNMATCHED) {
            return;
        }

        Conversation conversation = match.getConversation();
        if (conversation != null) {
            messageRepository.deleteByConversationId(conversation.getId());
            conversationRepository.deleteByIdOptimistic(conversation.getId());
        }

        match.setStatus(MatchStatus.UNMATCHED);
        match.setUnmatchedAt(LocalDateTime.now());
        matchRepository.save(match);
    }

    /**
     * Forme (ou réactive) le match de la paire (myPet, targetPet) et ouvre sa
     * conversation. Un match déjà actif sur la même paire n'est pas dupliqué.
     */
    private SwipeResponse handleMatch(User me, User targetOwner, Pet myPet, Pet targetPet) {
        Match match = latestForPair(myPet, targetPet);
        if (isInReMatchCooldown(match)) {
            // Re-match encore trop récent : on ne réactive rien, sans message.
            return SwipeResponse.notMatched(SwipeType.LIKE);
        }
        boolean notify = false;
        if (match == null) {
            match = Match.builder()
                    .status(MatchStatus.MATCHED)
                    .matchedAt(LocalDateTime.now())
                    .user1(me)
                    .user2(targetOwner)
                    .pet1(myPet)
                    .pet2(targetPet)
                    .build();
            matchRepository.save(match);
            notify = true;
        } else if (match.getStatus() == MatchStatus.UNMATCHED) {
            // La paire avait été supprimée : le processus repart de zéro.
            match.setStatus(MatchStatus.MATCHED);
            match.setMatchedAt(LocalDateTime.now());
            match.setUnmatchedAt(null);
            matchRepository.save(match);
            notify = true;
        }

        Conversation conversation = match.getConversation();
        if (conversation == null) {
            conversation = conversationRepository.save(Conversation.builder().match(match).build());
        }

        if (notify) {
            notifyMatch(me, targetOwner);
            notifyMatch(targetOwner, me);
        }

        return new SwipeResponse(true, match.getId(), conversation.getId(), SwipeType.LIKE);
    }

    private Match latestForPair(Pet myPet, Pet targetPet) {
        return matchRepository.findByPetPair(myPet.getId(), targetPet.getId())
                .stream().findFirst().orElse(null);
    }

    /**
     * Vrai si le dernier match de la paire a été supprimé il y a moins de
     * {@code re-match-cooldown-days} jours : le re-match est alors bloqué
     * silencieusement et la Pet cachée du feed.
     */
    private boolean isInReMatchCooldown(Match match) {
        if (match == null || match.getStatus() != MatchStatus.UNMATCHED
                || match.getUnmatchedAt() == null) {
            return false;
        }
        return match.getUnmatchedAt().isAfter(cooldownCutoff());
    }

    private LocalDateTime cooldownCutoff() {
        return LocalDateTime.now().minusDays(appProperties.getMatch().getReMatchCooldownDays());
    }

    private void notifyMatch(User recipient, User other) {
        notificationRepository.save(Notification.builder()
                .title("Nouveau match !")
                .content("Vous et " + other.getFirstName() + " êtes matchés. Une conversation est ouverte.")
                .type(NotificationType.MATCH)
                .user(recipient)
                .build());
    }

    private void upsertSwipe(User me, Pet targetPet, SwipeType type) {
        Swipe swipe = swipeRepository.findByUserIdAndPetId(me.getId(), targetPet.getId())
                .map(existing -> {
                    existing.setType(type);
                    return existing;
                })
                .orElseGet(() -> Swipe.builder()
                        .type(type)
                        .user(me)
                        .pet(targetPet)
                        .build());
        swipeRepository.save(swipe);
    }

    private SwipeCandidateResponse toCandidate(Pet pet, Map<Long, List<PetPhoto>> photosByPetId) {
        User owner = pet.getOwner();
        return new SwipeCandidateResponse(
                pet.getId(),
                pet.getName(),
                pet.getType(),
                pet.getBreed(),
                pet.getAge(),
                pet.getGender(),
                pet.getEnergyLevel(),
                primaryPhotoUrl(pet, photosByPetId.get(pet.getId())),
                new SwipeCandidateResponse.OwnerInfo(
                        owner.getId(), owner.getFirstName(), owner.getBio(), owner.getProfilePicture()));
    }

    private MatchResponse toMatchResponse(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getStatus(),
                match.getMatchedAt(),
                match.getConversation() != null ? match.getConversation().getId() : null,
                toPetSummary(match.getPet1()),
                toPetSummary(match.getPet2()));
    }

    private MatchResponse.PetSummary toPetSummary(Pet pet) {
        return new MatchResponse.PetSummary(
                pet.getId(), pet.getName(), primaryPhotoUrl(pet, pet.getPhotos()),
                pet.getType(), pet.getAge(), pet.getBreed());
    }

    private Map<Long, List<PetPhoto>> photoIndex(List<Pet> pets) {
        List<Long> petIds = pets.stream().map(Pet::getId).toList();
        return petPhotoRepository.findByPetIds(petIds).stream()
                .collect(Collectors.groupingBy(photo -> photo.getPet().getId()));
    }

    private static String primaryPhotoUrl(Pet pet, List<PetPhoto> photos) {
        if (photos == null || photos.isEmpty()) {
            return null;
        }
        return photos.stream()
                .filter(PetPhoto::isPrimaryPhoto)
                .findFirst()
                .orElse(photos.get(0))
                .getUrl();
    }

    /**
     * Filtre géographique par rayon de recherche de l'utilisateur (activé si
     * lat/long/rayon sont renseignés).
     */
    private boolean withinSearchRadius(User me, Pet pet) {
        if (me.getLatitude() == null || me.getLongitude() == null || me.getSearchRadius() == null) {
            return true;
        }
        User owner = pet.getOwner();
        if (owner.getLatitude() == null || owner.getLongitude() == null) {
            return false;
        }
        return haversineKm(me.getLatitude(), me.getLongitude(), owner.getLatitude(), owner.getLongitude())
                <= me.getSearchRadius();
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
    }
}