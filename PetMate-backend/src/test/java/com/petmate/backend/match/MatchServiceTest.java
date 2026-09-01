package com.petmate.backend.match;

import com.petmate.backend.config.AppProperties;
import com.petmate.backend.entity.Conversation;
import com.petmate.backend.entity.Match;
import com.petmate.backend.entity.Notification;
import com.petmate.backend.entity.Pet;
import com.petmate.backend.entity.PetPhoto;
import com.petmate.backend.entity.Swipe;
import com.petmate.backend.entity.User;
import com.petmate.backend.enums.EnergyLevel;
import com.petmate.backend.enums.MatchStatus;
import com.petmate.backend.enums.PetGender;
import com.petmate.backend.enums.PetType;
import com.petmate.backend.enums.SwipeType;
import com.petmate.backend.enums.UserRole;
import com.petmate.backend.exception.MatchNotFoundException;
import com.petmate.backend.exception.PetNotFoundException;
import com.petmate.backend.exception.RegistrationException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    private static final long ME_ID = 1L;
    private static final long TARGET_ID = 2L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private PetPhotoRepository petPhotoRepository;
    @Mock
    private SwipeRepository swipeRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private BlockRepository blockRepository;

    private AppProperties appProperties;
    private MatchService matchService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        matchService = new MatchService(
                userRepository, petRepository, petPhotoRepository, swipeRepository,
                matchRepository, conversationRepository, messageRepository,
                notificationRepository, blockRepository, appProperties);
    }

    // -------------------- Swipe --------------------

    @Test
    void swipe_withPASS_recordsSwipeWithoutMatch() {
        User me = user(ME_ID, "Alice");
        User target = user(TARGET_ID, "Bob");
        Pet targetPet = pet(2L, target, "Max.jpg", true);
        stubSwipeBasics(me, target, targetPet);

        SwipeResponse response = matchService.swipe(ME_ID, new SwipeRequest(SwipeType.PASS, 2L));

        assertFalse(response.matched());
        assertEquals(SwipeType.PASS, response.type());
        assertNull(response.matchId());
        assertNull(response.conversationId());
        ArgumentCaptor<Swipe> captor = ArgumentCaptor.forClass(Swipe.class);
        verify(swipeRepository).save(captor.capture());
        assertEquals(SwipeType.PASS, captor.getValue().getType());
        verify(notificationRepository, never()).save(any());
        verify(matchRepository, never()).save(any());
    }

    @Test
    void swipe_withLIKE_noReciprocal_recordsSwipeWithoutMatch() {
        User me = user(ME_ID, "Alice");
        User target = user(TARGET_ID, "Bob");
        Pet targetPet = pet(2L, target, "Max.jpg", true);
        stubSwipeBasics(me, target, targetPet);
        when(swipeRepository.findLikesOnPetsOfUser(TARGET_ID, ME_ID, SwipeType.LIKE)).thenReturn(List.of());

        SwipeResponse response = matchService.swipe(ME_ID, new SwipeRequest(SwipeType.LIKE, 2L));

        assertFalse(response.matched());
        assertEquals(SwipeType.LIKE, response.type());
        verify(swipeRepository).save(any(Swipe.class));
        verify(notificationRepository, never()).save(any());
        verify(matchRepository, never()).save(any());
    }

    @Test
    void swipe_withReciprocalLike_createsMatchConversationAndNotifications() {
        User me = user(ME_ID, "Alice");
        User target = user(TARGET_ID, "Bob");
        Pet myPet = pet(1L, me, "Rex.jpg", true);
        Pet targetPet = pet(2L, target, "Max.jpg", true);
        stubSwipeBasics(me, target, targetPet);
        when(swipeRepository.findLikesOnPetsOfUser(TARGET_ID, ME_ID, SwipeType.LIKE))
                .thenReturn(List.of(like(50L, target, myPet)));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> {
            Match m = inv.getArgument(0);
            m.setId(5L);
            return m;
        });
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(9L);
            return c;
        });

        SwipeResponse response = matchService.swipe(ME_ID, new SwipeRequest(SwipeType.LIKE, 2L));

        assertTrue(response.matched());
        assertEquals(5L, response.matchId());
        assertEquals(9L, response.conversationId());
        verify(matchRepository).save(any(Match.class));
        verify(conversationRepository).save(any(Conversation.class));
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void swipe_reactivatesUnmatchedPairAndCreatesConversation() {
        User me = user(ME_ID, "Alice");
        User target = user(TARGET_ID, "Bob");
        Pet myPet = pet(1L, me, "Rex.jpg", true);
        Pet targetPet = pet(2L, target, "Max.jpg", true);
        Match previous = Match.builder()
                .id(5L).status(MatchStatus.UNMATCHED)
                .user1(me).user2(target).pet1(myPet).pet2(targetPet)
                .build();
        stubSwipeBasics(me, target, targetPet);
        when(swipeRepository.findLikesOnPetsOfUser(TARGET_ID, ME_ID, SwipeType.LIKE))
                .thenReturn(List.of(like(50L, target, myPet)));
        when(matchRepository.findByPetPair(anyLong(), anyLong())).thenReturn(List.of(previous));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(9L);
            return c;
        });

        SwipeResponse response = matchService.swipe(ME_ID, new SwipeRequest(SwipeType.LIKE, 2L));

        assertTrue(response.matched());
        assertEquals(MatchStatus.MATCHED, previous.getStatus());
        assertNotNull(previous.getMatchedAt());
        assertNull(previous.getUnmatchedAt());
        assertEquals(9L, response.conversationId());
        verify(matchRepository).save(previous);
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void swipe_reMatchWithinCooldown_doesNotReactivateSilently() {
        User me = user(ME_ID, "Alice");
        User target = user(TARGET_ID, "Bob");
        Pet myPet = pet(1L, me, "Rex.jpg", true);
        Pet targetPet = pet(2L, target, "Max.jpg", true);
        Match recent = Match.builder()
                .id(5L).status(MatchStatus.UNMATCHED)
                .unmatchedAt(LocalDateTime.now().minusDays(5))
                .user1(me).user2(target).pet1(myPet).pet2(targetPet)
                .build();
        stubSwipeBasics(me, target, targetPet);
        when(swipeRepository.findLikesOnPetsOfUser(TARGET_ID, ME_ID, SwipeType.LIKE))
                .thenReturn(List.of(like(50L, target, myPet)));
        when(matchRepository.findByPetPair(anyLong(), anyLong())).thenReturn(List.of(recent));

        SwipeResponse response = matchService.swipe(ME_ID, new SwipeRequest(SwipeType.LIKE, 2L));

        assertFalse(response.matched());
        assertEquals(MatchStatus.UNMATCHED, recent.getStatus());
        verify(matchRepository, never()).save(any());
        verify(conversationRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void swipe_reMatchAfterCooldown_reactivatesPair() {
        User me = user(ME_ID, "Alice");
        User target = user(TARGET_ID, "Bob");
        Pet myPet = pet(1L, me, "Rex.jpg", true);
        Pet targetPet = pet(2L, target, "Max.jpg", true);
        Match old = Match.builder()
                .id(5L).status(MatchStatus.UNMATCHED)
                .unmatchedAt(LocalDateTime.now().minusDays(30))
                .user1(me).user2(target).pet1(myPet).pet2(targetPet)
                .build();
        stubSwipeBasics(me, target, targetPet);
        when(swipeRepository.findLikesOnPetsOfUser(TARGET_ID, ME_ID, SwipeType.LIKE))
                .thenReturn(List.of(like(50L, target, myPet)));
        when(matchRepository.findByPetPair(anyLong(), anyLong())).thenReturn(List.of(old));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(9L);
            return c;
        });

        SwipeResponse response = matchService.swipe(ME_ID, new SwipeRequest(SwipeType.LIKE, 2L));

        assertTrue(response.matched());
        assertEquals(MatchStatus.MATCHED, old.getStatus());
        assertNull(old.getUnmatchedAt());
        verify(matchRepository).save(old);
    }

    @Test
    void swipe_pairAlreadyMatched_doesNotDuplicateMatchOrNotify() {
        User me = user(ME_ID, "Alice");
        User target = user(TARGET_ID, "Bob");
        Pet myPet = pet(1L, me, "Rex.jpg", true);
        Pet targetPet = pet(2L, target, "Max.jpg", true);
        Match active = Match.builder()
                .id(5L).status(MatchStatus.MATCHED)
                .matchedAt(LocalDateTime.now())
                .user1(me).user2(target).pet1(myPet).pet2(targetPet)
                .build();
        active.setConversation(conversation(9L));
        stubSwipeBasics(me, target, targetPet);
        when(swipeRepository.findLikesOnPetsOfUser(TARGET_ID, ME_ID, SwipeType.LIKE))
                .thenReturn(List.of(like(50L, target, myPet)));
        when(matchRepository.findByPetPair(anyLong(), anyLong())).thenReturn(List.of(active));

        SwipeResponse response = matchService.swipe(ME_ID, new SwipeRequest(SwipeType.LIKE, 2L));

        assertTrue(response.matched());
        assertEquals(5L, response.matchId());
        assertEquals(9L, response.conversationId());
        verify(matchRepository, never()).save(any());
        verify(conversationRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void swipe_oneSwiftLikeCanMatchSeveralOfMyPets() {
        User me = user(ME_ID, "Alice");
        User target = user(TARGET_ID, "Bob");
        Pet myPet1 = pet(1L, me, "Rex.jpg", true);
        Pet myPet2 = pet(3L, me, "Nyx.jpg", true);
        Pet targetPet = pet(2L, target, "Max.jpg", true);
        stubSwipeBasics(me, target, targetPet);
        when(swipeRepository.findLikesOnPetsOfUser(TARGET_ID, ME_ID, SwipeType.LIKE))
                .thenReturn(List.of(like(50L, target, myPet1), like(51L, target, myPet2)));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(9L);
            return c;
        });

        SwipeResponse response = matchService.swipe(ME_ID, new SwipeRequest(SwipeType.LIKE, 2L));

        assertTrue(response.matched());
        verify(matchRepository, times(2)).save(any(Match.class));
        verify(conversationRepository, times(2)).save(any(Conversation.class));
        verify(notificationRepository, times(4)).save(any(Notification.class));
    }

    @Test
    void swipe_passAfterExistingLike_updatesTypeAndNoMatch() {
        User me = user(ME_ID, "Alice");
        User target = user(TARGET_ID, "Bob");
        Pet targetPet = pet(2L, target, "Max.jpg", true);
        Swipe existingLike = Swipe.builder().id(7L).type(SwipeType.LIKE).user(me).pet(targetPet).build();
        stubSwipeBasics(me, target, targetPet);
        when(swipeRepository.findByUserIdAndPetId(ME_ID, targetPet.getId()))
                .thenReturn(Optional.of(existingLike));

        SwipeResponse response = matchService.swipe(ME_ID, new SwipeRequest(SwipeType.PASS, 2L));

        assertFalse(response.matched());
        assertEquals(SwipeType.PASS, existingLike.getType());
        verify(swipeRepository).save(existingLike);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void swipe_withoutActivePet_throws() {
        User me = user(ME_ID, "Alice");
        when(userRepository.findById(ME_ID)).thenReturn(Optional.of(me));
        when(petRepository.existsByOwnerIdAndActiveTrue(ME_ID)).thenReturn(false);

        assertThrows(RegistrationException.class,
                () -> matchService.swipe(ME_ID, new SwipeRequest(SwipeType.LIKE, 2L)));
    }

    @Test
    void swipe_unknownTargetPet_throws() {
        User me = user(ME_ID, "Alice");
        when(userRepository.findById(ME_ID)).thenReturn(Optional.of(me));
        when(petRepository.existsByOwnerIdAndActiveTrue(ME_ID)).thenReturn(true);
        when(petRepository.findByIdWithPhotos(99L)).thenReturn(Optional.empty());

        assertThrows(PetNotFoundException.class,
                () -> matchService.swipe(ME_ID, new SwipeRequest(SwipeType.LIKE, 99L)));
    }

    @Test
    void swipe_ownPet_throws() {
        User me = user(ME_ID, "Alice");
        Pet myPet = pet(1L, me, "Rex.jpg", true);
        stubSwipeBasics(me, me, myPet);

        assertThrows(PetNotFoundException.class,
                () -> matchService.swipe(ME_ID, new SwipeRequest(SwipeType.LIKE, 1L)));
    }

    @Test
    void swipe_inactiveTargetPet_throws() {
        User me = user(ME_ID, "Alice");
        User target = user(TARGET_ID, "Bob");
        Pet targetPet = pet(2L, target, "Max.jpg", false);
        stubSwipeBasics(me, target, targetPet);

        assertThrows(PetNotFoundException.class,
                () -> matchService.swipe(ME_ID, new SwipeRequest(SwipeType.LIKE, 2L)));
    }

    @Test
    void swipe_blockedOwner_throws() {
        User me = user(ME_ID, "Alice");
        User target = user(TARGET_ID, "Bob");
        Pet targetPet = pet(2L, target, "Max.jpg", true);
        stubSwipeBasics(me, target, targetPet);
        when(blockRepository.existBetweenOwners(ME_ID, TARGET_ID)).thenReturn(true);

        assertThrows(PetNotFoundException.class,
                () -> matchService.swipe(ME_ID, new SwipeRequest(SwipeType.LIKE, 2L)));
    }

    // -------------------- Feed --------------------

    @Test
    void candidates_returnsEveryActivePetEvenFromSameOwner() {
        User me = user(ME_ID, "Alice");
        User ownerA = user(11L, "Carol");
        User ownerB = user(12L, "Dave");
        Pet a1 = pet(1L, ownerA, "carol-main.jpg", true);
        Pet a2 = pet(2L, ownerA, "carol-alt.jpg", true);
        Pet b1 = pet(3L, ownerB, "dave.jpg", true);

        when(userRepository.findById(ME_ID)).thenReturn(Optional.of(me));
        when(petRepository.findCandidates(eq(ME_ID), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a1, a2, b1)));
        when(petPhotoRepository.findByPetIds(anyCollection()))
                .thenReturn(List.of(
                        photo(10L, "carol-main.jpg", true, a1),
                        photo(11L, "carol-alt.jpg", false, a2),
                        photo(12L, "dave.jpg", true, b1)));

        List<SwipeCandidateResponse> candidates = matchService.candidates(ME_ID, 10, 0);

        assertEquals(3, candidates.size());
        assertEquals(1L, candidates.get(0).petId());
        assertEquals("carol-main.jpg", candidates.get(0).primaryPhotoUrl());
        assertEquals(2L, candidates.get(1).petId());
        assertEquals("Dave", candidates.get(2).owner().firstName());
        assertNull(candidates.get(2).owner().bio());
    }

    @Test
    void candidates_filtersOwnersOutsideSearchRadius() {
        User me = user(ME_ID, "Alice");
        me.setLatitude(48.85);
        me.setLongitude(2.35);
        me.setSearchRadius(10);
        User near = user(11L, "Near");
        near.setLatitude(48.86);
        near.setLongitude(2.36);
        User far = user(12L, "Far");
        far.setLatitude(40.70);
        far.setLongitude(-74.00);
        User noGeo = user(13L, "NoGeo");
        Pet nearPet = pet(1L, near, "near.jpg", true);
        Pet farPet = pet(2L, far, "far.jpg", true);
        Pet noGeoPet = pet(3L, noGeo, "nogeo.jpg", true);

        when(userRepository.findById(ME_ID)).thenReturn(Optional.of(me));
        when(petRepository.findCandidates(eq(ME_ID), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(nearPet, farPet, noGeoPet)));
        when(petPhotoRepository.findByPetIds(anyCollection()))
                .thenReturn(List.of(
                        photo(20L, "near.jpg", true, nearPet),
                        photo(21L, "far.jpg", true, farPet),
                        photo(22L, "nogeo.jpg", true, noGeoPet)));

        List<SwipeCandidateResponse> candidates = matchService.candidates(ME_ID, 10, 0);

        assertEquals(1, candidates.size());
        assertEquals("near.jpg", candidates.get(0).primaryPhotoUrl());
    }

    // -------------------- Mes matchs --------------------

    @Test
    void myMatches_returnsOnlyMatchedWithPetsAndConversation() {
        User me = user(ME_ID, "Alice");
        User other = user(TARGET_ID, "Bob");
        Pet myPet = pet(1L, me, "Rex.jpg", true);
        Pet otherPet = pet(2L, other, "Max.jpg", true);
        Match matched = matchedBetween(me, other, myPet, otherPet);
        matched.setConversation(conversation(9L));
        Match unmatched = Match.builder()
                .id(7L).status(MatchStatus.UNMATCHED)
                .user1(me).user2(other).pet1(myPet).pet2(otherPet)
                .build();

        when(matchRepository.findForUser(ME_ID)).thenReturn(List.of(matched, unmatched));

        List<MatchResponse> response = matchService.myMatches(ME_ID);

        assertEquals(1, response.size());
        MatchResponse match = response.get(0);
        assertEquals(9L, match.conversationId());
        assertEquals("Rex", match.pet1().petName());
        assertEquals("Rex.jpg", match.pet1().primaryPhotoUrl());
        assertEquals("Max", match.pet2().petName());
    }

    // -------------------- Unmatch --------------------

    @Test
    void unmatch_participant_deletesConversationAndSetsUnmatched() {
        User me = user(ME_ID, "Alice");
        User other = user(TARGET_ID, "Bob");
        Pet myPet = pet(1L, me, "Rex.jpg", true);
        Pet otherPet = pet(2L, other, "Max.jpg", true);
        Match match = matchedBetween(me, other, myPet, otherPet);
        match.setConversation(conversation(9L));
        when(matchRepository.findById(5L)).thenReturn(Optional.of(match));

        matchService.unmatch(ME_ID, 5L);

        verify(messageRepository).deleteByConversationId(9L);
        verify(conversationRepository).deleteByIdOptimistic(9L);
        assertEquals(MatchStatus.UNMATCHED, match.getStatus());
        assertNotNull(match.getUnmatchedAt());
        verify(matchRepository).save(match);
    }

    @Test
    void unmatch_nonParticipant_throws() {
        User stranger = user(99L, "Eve");
        Match match = Match.builder()
                .id(5L).status(MatchStatus.MATCHED)
                .user1(stranger).user2(stranger)
                .pet1(pet(1L, stranger, "x.jpg", true))
                .pet2(pet(2L, stranger, "y.jpg", true))
                .build();
        when(matchRepository.findById(5L)).thenReturn(Optional.of(match));

        assertThrows(MatchNotFoundException.class, () -> matchService.unmatch(ME_ID, 5L));
        verify(conversationRepository, never()).deleteByIdOptimistic(anyLong());
    }

    @Test
    void unmatch_unknownMatch_throws() {
        when(matchRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(MatchNotFoundException.class, () -> matchService.unmatch(ME_ID, 5L));
    }

    @Test
    void unmatch_alreadyUnmatched_isIdempotent() {
        User me = user(ME_ID, "Alice");
        User other = user(TARGET_ID, "Bob");
        Match match = Match.builder()
                .id(5L).status(MatchStatus.UNMATCHED)
                .user1(me).user2(other)
                .pet1(pet(1L, me, "x.jpg", true))
                .pet2(pet(2L, other, "y.jpg", true))
                .build();
        when(matchRepository.findById(5L)).thenReturn(Optional.of(match));

        matchService.unmatch(ME_ID, 5L);

        verify(messageRepository, never()).deleteByConversationId(anyLong());
        verify(conversationRepository, never()).deleteByIdOptimistic(anyLong());
        verify(matchRepository, never()).save(any());
    }

    // -------------------- Helpers --------------------

    private void stubSwipeBasics(User me, User target, Pet targetPet) {
        lenient().when(userRepository.findById(ME_ID)).thenReturn(Optional.of(me));
        lenient().when(petRepository.existsByOwnerIdAndActiveTrue(ME_ID)).thenReturn(true);
        lenient().when(petRepository.findByIdWithPhotos(targetPet.getId())).thenReturn(Optional.of(targetPet));
    }

    private User user(long id, String firstName) {
        return User.builder()
                .id(id).firstName(firstName).lastName("LastName")
                .email("user" + id + "@example.com").password("secret")
                .role(UserRole.USER).active(true).build();
    }

    private Pet pet(long id, User owner, String photoUrl, boolean active) {
        String name;
        switch ((int) id) {
            case 1: name = "Rex"; break;
            case 2: name = "Max"; break;
            case 3: name = "Nyx"; break;
            default: name = "Pet" + id;
        }
        Pet pet = Pet.builder()
                .id(id).name(name).type(PetType.DOG).breed("Labrador")
                .gender(PetGender.MALE).age(3).energyLevel(EnergyLevel.HIGH)
                .active(active).owner(owner).build();
        pet.getPhotos().add(photo(id * 100, photoUrl, true, pet));
        return pet;
    }

    private PetPhoto photo(long id, String url, boolean primary, Pet pet) {
        return PetPhoto.builder()
                .id(id).url(url).primaryPhoto(primary).displayOrder(0).pet(pet).build();
    }

    private Swipe like(long id, User user, Pet pet) {
        return Swipe.builder().id(id).type(SwipeType.LIKE).user(user).pet(pet).build();
    }

    private Match matchedBetween(User me, User other, Pet myPet, Pet otherPet) {
        return Match.builder()
                .id(5L).status(MatchStatus.MATCHED)
                .matchedAt(LocalDateTime.now())
                .user1(me).user2(other).pet1(myPet).pet2(otherPet)
                .build();
    }

    private Conversation conversation(long id) {
        return Conversation.builder().id(id).build();
    }
}