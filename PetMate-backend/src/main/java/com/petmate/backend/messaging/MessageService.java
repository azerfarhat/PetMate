package com.petmate.backend.messaging;

import com.petmate.backend.entity.Conversation;
import com.petmate.backend.entity.Match;
import com.petmate.backend.entity.Message;
import com.petmate.backend.entity.Notification;
import com.petmate.backend.entity.Pet;
import com.petmate.backend.entity.PetPhoto;
import com.petmate.backend.entity.User;
import com.petmate.backend.enums.MatchStatus;
import com.petmate.backend.enums.NotificationType;
import com.petmate.backend.exception.ConversationNotFoundException;
import com.petmate.backend.messaging.dto.ConversationResponse;
import com.petmate.backend.messaging.dto.MessageResponse;
import com.petmate.backend.messaging.dto.SendMessageRequest;
import com.petmate.backend.notification.dto.NotificationResponse;
import com.petmate.backend.repository.BlockRepository;
import com.petmate.backend.repository.ConversationRepository;
import com.petmate.backend.repository.MessageRepository;
import com.petmate.backend.repository.NotificationRepository;
import com.petmate.backend.repository.PetPhotoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Messagerie post-match : un échange ne se fait que dans une conversation ouverte
 * par un match actif (MATCHED), et uniquement par ses deux participants.
 *
 * Règles de sécurité appliquées dans ce service (jamais de contrôle chez un
 * participant externe) :
 * <ul>
 *   <li>sécurité d'appartenance : le match est chargé avec ses deux participants,
 *       un non-participant reçoit un 404 volontairement ambigu ;</li>
 *   <li>un match UNMATCHED n'est plus accessible (sa conversation est supprimée
 *       à l'unmatch, le statut est revérifié par précaution) ;</li>
 *   <li>blocage : lorsque l'un des deux a bloqué l'autre, l'envoi est rejeté
 *       silencieusement (404) — aucune information ni aucun message ne part.</li>
 * </ul>
 */
@Service
public class MessageService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_NOTIFICATION_LENGTH = 2000;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final BlockRepository blockRepository;
    private final PetPhotoRepository petPhotoRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public MessageService(ConversationRepository conversationRepository,
                          MessageRepository messageRepository,
                          NotificationRepository notificationRepository,
                          BlockRepository blockRepository,
                          PetPhotoRepository petPhotoRepository,
                          ApplicationEventPublisher applicationEventPublisher) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
        this.blockRepository = blockRepository;
        this.petPhotoRepository = petPhotoRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Conversations accessibles de l'utilisateur, triées par dernière activité
     * (conversations sans message en dernier). Le dernier message et le nombre de
     * non-lus sont dérivés en une seule requête pour toute la page.
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(Long userId, int limit, int offset) {
        Pageable pageable = PageRequest.of(pageOf(offset, limit), sizeOf(limit),
                Sort.by(Sort.Order.desc("lastMessageAt").with(Sort.NullHandling.NULLS_LAST)));
        List<Conversation> conversations = conversationRepository.findAllForUserWithDetails(userId, pageable);
        if (conversations.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Message>> messagesByConversation = messagesOf(conversations);
        Map<Long, List<PetPhoto>> photosByPetId = photosOf(conversations);

        return conversations.stream()
                .map(conversation -> toConversationResponse(
                        userId, conversation,
                        messagesByConversation.getOrDefault(conversation.getId(), List.of()),
                        photosByPetId))
                .toList();
    }

    /**
     * Historique d'une conversation, paginé par date d'envoi.
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long userId, Long conversationId, int limit, int offset) {
        requireAccessibleConversation(userId, conversationId);
        return messageRepository.findPageByConversationIdWithSender(
                        conversationId, PageRequest.of(pageOf(offset, limit), sizeOf(limit)))
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    /**
     * Envoi d'un message : persistance, mise à jour de la dernière activité et
     * notification MESSAGE chez l'interlocuteur, dans une seule transaction.
     * L'événement de diffusion temps réel est émis après le commit.
     */
    @Transactional
    public MessageResponse sendMessage(Long userId, Long conversationId, SendMessageRequest request) {
        Conversation conversation = requireAccessibleConversation(userId, conversationId);
        User sender = participant(conversation, userId);
        User recipient = opponent(conversation, userId);

        if (blockRepository.existBetweenOwners(userId, recipient.getId())) {
            throw new ConversationNotFoundException("Conversation introuvable");
        }

        Message message = messageRepository.save(Message.builder()
                .content(request.content().trim())
                .conversation(conversation)
                .sender(sender)
                .build());

        conversation.setLastMessageAt(message.getSentAt());

        Notification notification = notificationRepository.save(Notification.builder()
                .title("Nouveau message")
                .content(truncate(message.getContent(), MAX_NOTIFICATION_LENGTH))
                .type(NotificationType.MESSAGE)
                .user(recipient)
                .build());

        MessageResponse response = toMessageResponse(message);
        applicationEventPublisher.publishEvent(new MessagePublishedEvent(
                response, recipient.getId(), toNotificationResponse(notification)));
        return response;
    }

    /**
     * Marque comme lus les messages envoyés par l'interlocuteur. Idempotent :
     * sans message non lu, aucune écriture.
     */
    @Transactional
    public void markRead(Long userId, Long conversationId) {
        requireAccessibleConversation(userId, conversationId);
        List<Message> unread = messageRepository.findUnreadFromOther(conversationId, userId);
        if (unread.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(message -> {
            message.setRead(true);
            message.setReadAt(now);
        });
        messageRepository.saveAll(unread);
    }

    private Conversation requireAccessibleConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation introuvable"));
        Match match = conversation.getMatch();
        boolean participant = match.getUser1().getId().equals(userId)
                || match.getUser2().getId().equals(userId);
        if (!participant || match.getStatus() != MatchStatus.MATCHED) {
            throw new ConversationNotFoundException("Conversation introuvable");
        }
        return conversation;
    }

    private ConversationResponse toConversationResponse(long userId,
                                                        Conversation conversation,
                                                        List<Message> messages,
                                                        Map<Long, List<PetPhoto>> photosByPetId) {
        Match match = conversation.getMatch();
        boolean iAmUser1 = match.getUser1().getId().equals(userId);
        User other = iAmUser1 ? match.getUser2() : match.getUser1();
        Pet otherPet = iAmUser1 ? match.getPet2() : match.getPet1();

        Message last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        long unreadCount = messages.stream()
                .filter(message -> !message.isRead() && !message.getSender().getId().equals(userId))
                .count();

        return new ConversationResponse(
                conversation.getId(),
                match.getId(),
                other.getId(),
                other.getFirstName(),
                other.getProfilePicture(),
                otherPet.getId(),
                otherPet.getName(),
                primaryPhotoUrl(otherPet, photosByPetId.get(otherPet.getId())),
                last == null ? null : new ConversationResponse.MessagePreview(
                        last.getId(), last.getContent(), last.getSender().getId(),
                        last.getSentAt(), last.isRead()),
                unreadCount);
    }

    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getContent(),
                message.getSender().getId(),
                message.getSentAt(),
                message.isRead());
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt());
    }

    private User participant(Conversation conversation, Long userId) {
        Match match = conversation.getMatch();
        return match.getUser1().getId().equals(userId) ? match.getUser1() : match.getUser2();
    }

    private User opponent(Conversation conversation, Long userId) {
        Match match = conversation.getMatch();
        return match.getUser1().getId().equals(userId) ? match.getUser2() : match.getUser1();
    }

    private Map<Long, List<Message>> messagesOf(List<Conversation> conversations) {
        List<Long> conversationIds = conversations.stream().map(Conversation::getId).toList();
        return messageRepository.findByConversationIds(conversationIds).stream()
                .collect(Collectors.groupingBy(message -> message.getConversation().getId()));
    }

    private Map<Long, List<PetPhoto>> photosOf(List<Conversation> conversations) {
        List<Long> petIds = new ArrayList<>();
        for (Conversation conversation : conversations) {
            petIds.add(conversation.getMatch().getPet1().getId());
            petIds.add(conversation.getMatch().getPet2().getId());
        }
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

    private static String truncate(String value, int max) {
        if (value.codePointCount(0, value.length()) <= max) {
            return value;
        }
        return value.codePoints()
                .limit(max)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private static int sizeOf(int limit) {
        return Math.min(Math.max(1, limit), MAX_PAGE_SIZE);
    }

    private static int pageOf(int offset, int limit) {
        return Math.max(0, offset) / Math.max(1, limit);
    }
}