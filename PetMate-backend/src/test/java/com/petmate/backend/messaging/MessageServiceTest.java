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
import com.petmate.backend.messaging.dto.MessagePageResponse;
import com.petmate.backend.messaging.dto.MessageResponse;
import com.petmate.backend.messaging.dto.SendMessageRequest;
import com.petmate.backend.messaging.websocket.MessageEventPublisher;
import com.petmate.backend.repository.BlockRepository;
import com.petmate.backend.repository.ConversationRepository;
import com.petmate.backend.repository.MessageRepository;
import com.petmate.backend.repository.NotificationRepository;
import com.petmate.backend.repository.PetPhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Règles de la messagerie : accès réservé aux participants d'un match actif,
 * rejet silencieux en cas de blocage, notification à l'interlocuteur, comptage
 * des non-lus et marquage "lu" idempotent.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    private static final long ME = 1L;
    private static final long OTHER = 2L;
    private static final long CONVERSATION_ID = 100L;
    private static final long MATCH_ID = 50L;

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private PetPhotoRepository petPhotoRepository;
    @Mock
    private MessageEventPublisher messageEventPublisher;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private MessageService messageService;

    private User me;
    private User other;
    private Pet myPet;
    private Pet otherPet;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(conversationRepository, messageRepository,
                notificationRepository, blockRepository, petPhotoRepository,
                messageEventPublisher, applicationEventPublisher);
        me = user(ME, "Jane");
        other = user(OTHER, "Bob");
        myPet = pet(11L, me, "Rex");
        otherPet = pet(12L, other, "Milo");
        conversation = conversation(CONVERSATION_ID, match(me, other, myPet, otherPet, MatchStatus.MATCHED));
    }

    @Test
    void listConversations_returnsMineWithLastMessageAndUnreadCount() {
        when(conversationRepository.findAllForUserWithDetails(eq(ME), any(Pageable.class)))
                .thenReturn(List.of(conversation));
        LocalDateTime t = LocalDateTime.now();
        Message hello = message(1L, other, "Coucou", t.minusMinutes(5), false);
        Message reply = message(2L, me, "Salut", t, true);
        when(messageRepository.findByConversationIds(any())).thenReturn(List.of(hello, reply));
        when(petPhotoRepository.findByPetIds(any())).thenReturn(List.of(photo(9L, otherPet)));

        List<ConversationResponse> result = messageService.listConversations(ME, 20, 0);

        assertEquals(1, result.size());
        ConversationResponse response = result.get(0);
        assertEquals(CONVERSATION_ID, response.conversationId());
        assertEquals(MATCH_ID, response.matchId());
        assertEquals(OTHER, response.otherUserId());
        assertEquals("Bob", response.otherFirstName());
        assertEquals(otherPet.getId(), response.otherPetId());
        assertEquals("Milo", response.otherPetName());
        assertEquals("https://cdn/milo.jpg", response.otherPetPrimaryPhotoUrl());
        assertNotNull(response.lastMessage());
        assertEquals("Salut", response.lastMessage().content());
        assertEquals(ME, response.lastMessage().senderId());
        assertTrue(response.lastMessage().read());
        assertEquals(1, response.unreadCount());
    }

    @Test
    void listConversations_emptyConversation_returnsNullPreviewAndZeroUnread() {
        when(conversationRepository.findAllForUserWithDetails(eq(ME), any(Pageable.class)))
                .thenReturn(List.of(conversation));
        when(messageRepository.findByConversationIds(any())).thenReturn(List.of());
        when(petPhotoRepository.findByPetIds(any())).thenReturn(List.of());

        List<ConversationResponse> result = messageService.listConversations(ME, 20, 0);

        assertEquals(1, result.size());
        assertNull(result.get(0).lastMessage());
        assertEquals(0, result.get(0).unreadCount());
    }

    @Test
    void listConversations_withoutConversation_returnsEmpty() {
        when(conversationRepository.findAllForUserWithDetails(eq(ME), any(Pageable.class)))
                .thenReturn(List.of());

        List<ConversationResponse> result = messageService.listConversations(ME, 20, 0);

        assertTrue(result.isEmpty());
        verify(messageRepository, never()).findByConversationIds(any());
    }

    @Test
    void sendMessage_savesMessageUpdatesLastActivityAndNotifiesRecipient() {
        when(conversationRepository.findByIdWithParticipants(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(blockRepository.existBetweenOwners(ME, OTHER)).thenReturn(false);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            saved.setId(300L);
            saved.setSentAt(LocalDateTime.now());
            return saved;
        });
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = messageService.sendMessage(ME, CONVERSATION_ID, new SendMessageRequest("Bonjour"));

        assertEquals(300L, response.id());
        assertEquals(CONVERSATION_ID, response.conversationId());
        assertEquals(ME, response.senderId());
        assertEquals("Bonjour", response.content());

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertEquals("Bonjour", messageCaptor.getValue().getContent());
        assertEquals(ME, messageCaptor.getValue().getSender().getId());

        assertEquals(response.sentAt(), conversation.getLastMessageAt());

        ArgumentCaptor<MessagePublishedEvent> eventCaptor = ArgumentCaptor.forClass(MessagePublishedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        MessagePublishedEvent event = eventCaptor.getValue();
        assertEquals(response.id(), event.message().id());
        assertEquals(OTHER, event.recipientUserId());
        assertNotNull(event.notification());
        assertEquals(NotificationType.MESSAGE, event.notification().type());

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertEquals(NotificationType.MESSAGE, notification.getType());
        assertEquals(OTHER, notification.getUser().getId());
        assertEquals("Nouveau message", notification.getTitle());
    }

    @Test
    void sendMessage_byNonParticipant_throwsNotFound() {
        Conversation foreign = conversation(200L, match(user(3L, "Eve"), other, pet(31L, null, "Nix"), otherPet, MatchStatus.MATCHED));
        when(conversationRepository.findByIdWithParticipants(200L)).thenReturn(Optional.of(foreign));

        assertThrows(ConversationNotFoundException.class,
                () -> messageService.sendMessage(ME, 200L, new SendMessageRequest("Bonjour")));
        verify(messageRepository, never()).save(any(Message.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void sendMessage_unknownConversation_throwsNotFound() {
        when(conversationRepository.findByIdWithParticipants(CONVERSATION_ID)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class,
                () -> messageService.sendMessage(ME, CONVERSATION_ID, new SendMessageRequest("Bonjour")));
    }

    @Test
    void sendMessage_onUnmatchedMatch_throwsNotFound() {
        Conversation unmatched = conversation(CONVERSATION_ID, match(me, other, myPet, otherPet, MatchStatus.UNMATCHED));
        when(conversationRepository.findByIdWithParticipants(CONVERSATION_ID)).thenReturn(Optional.of(unmatched));

        assertThrows(ConversationNotFoundException.class,
                () -> messageService.sendMessage(ME, CONVERSATION_ID, new SendMessageRequest("Bonjour")));
    }

    @Test
    void sendMessage_whenBlocked_throwsNotFoundAndPersistsNothing() {
        when(conversationRepository.findByIdWithParticipants(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(blockRepository.existBetweenOwners(ME, OTHER)).thenReturn(true);

        assertThrows(ConversationNotFoundException.class,
                () -> messageService.sendMessage(ME, CONVERSATION_ID, new SendMessageRequest("Bonjour")));
        verify(messageRepository, never()).save(any(Message.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void getMessagePage_returnsNewestPageWithCursorAndHasMore() {
        when(conversationRepository.findByIdWithParticipants(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        LocalDateTime t = LocalDateTime.now();
        Message m1 = message(1L, other, "Premier", t.minusMinutes(10), true);
        Message m2 = message(2L, me, "Deuxième", t.minusMinutes(5), true);
        Message m3 = message(3L, other, "Troisième", t, false);
        when(messageRepository.findNewestPage(eq(CONVERSATION_ID), any(Pageable.class)))
                .thenReturn(List.of(m3, m2, m1, message(0L, other, "Encore plus vieux", t.minusMinutes(15), true)));

        MessagePageResponse result = messageService.getMessagePage(ME, CONVERSATION_ID, 3, null);

        assertEquals(3, result.messages().size());
        assertEquals(1L, result.messages().get(0).id());
        assertEquals(3L, result.messages().get(2).id());
        assertTrue(result.hasMore());
        assertEquals(1L, result.nextCursor());
    }

    @Test
    void getMessagePage_withCursorReturnsOlderMessages() {
        when(conversationRepository.findByIdWithParticipants(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        LocalDateTime t = LocalDateTime.now();
        List<Message> older = List.of(
                message(5L, other, "Cinq", t.minusMinutes(20), true),
                message(6L, me, "Six", t.minusMinutes(15), true));
        when(messageRepository.findOlderThan(eq(CONVERSATION_ID), eq(10L), any(Pageable.class)))
                .thenReturn(older);

        MessagePageResponse result = messageService.getMessagePage(ME, CONVERSATION_ID, 50, 10L);

        assertEquals(2, result.messages().size());
        assertEquals(5L, result.messages().get(0).id());
        assertEquals(6L, result.messages().get(1).id());
        assertEquals(5L, result.nextCursor());
    }

    @Test
    void getMessagePage_clampsLimitToMaximumPageSize() {
        when(conversationRepository.findByIdWithParticipants(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findNewestPage(eq(CONVERSATION_ID), any(Pageable.class)))
                .thenReturn(List.of());

        MessagePageResponse result = messageService.getMessagePage(ME, CONVERSATION_ID, 500, null);

        verify(messageRepository).findNewestPage(eq(CONVERSATION_ID), argThat(
                pageable -> pageable.getPageSize() == 51));
        assertTrue(result.messages().isEmpty());
    }

    @Test
    void getMessagePage_byNonParticipant_throwsNotFoundAndQueriesNothing() {
        Conversation foreign = conversation(200L, match(user(3L, "Eve"), other, pet(31L, null, "Nix"), otherPet, MatchStatus.MATCHED));
        when(conversationRepository.findByIdWithParticipants(200L)).thenReturn(Optional.of(foreign));

        assertThrows(ConversationNotFoundException.class,
                () -> messageService.getMessagePage(ME, 200L, 50, null));
        verify(messageRepository, never()).findNewestPage(any(), any());
        verify(messageRepository, never()).findOlderThan(any(), any(), any());
    }

    @Test
    void getMessagePage_unknownConversation_throwsNotFound() {
        when(conversationRepository.findByIdWithParticipants(CONVERSATION_ID)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class,
                () -> messageService.getMessagePage(ME, CONVERSATION_ID, 50, null));
    }

    @Test
    void typing_publishesOnlyToOpponent() {
        when(conversationRepository.findByIdWithParticipants(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(blockRepository.existBetweenOwners(ME, OTHER)).thenReturn(false);

        messageService.typing(ME, CONVERSATION_ID, true);

        verify(messageEventPublisher).publishTyping(CONVERSATION_ID, ME, OTHER, true);
    }

    @Test
    void typing_byNonParticipant_throwsNotFoundAndPublishesNothing() {
        Conversation foreign = conversation(200L, match(user(3L, "Eve"), other, pet(31L, null, "Nix"), otherPet, MatchStatus.MATCHED));
        when(conversationRepository.findByIdWithParticipants(200L)).thenReturn(Optional.of(foreign));

        assertThrows(ConversationNotFoundException.class,
                () -> messageService.typing(ME, 200L, true));
        verify(messageEventPublisher, never()).publishTyping(any(), any(), any(), anyBoolean());
    }

    @Test
    void typing_whenBlocked_throwsNotFoundAndPublishesNothing() {
        when(conversationRepository.findByIdWithParticipants(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(blockRepository.existBetweenOwners(ME, OTHER)).thenReturn(true);

        assertThrows(ConversationNotFoundException.class,
                () -> messageService.typing(ME, CONVERSATION_ID, true));
        verify(messageEventPublisher, never()).publishTyping(any(), any(), any(), anyBoolean());
    }

    @Test
    void markRead_marksOnlyMessagesFromOther() {
        when(conversationRepository.findByIdWithParticipants(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        Message fromOther = message(1L, other, "Coucou", LocalDateTime.now().minusMinutes(1), false);
        when(messageRepository.findUnreadFromOther(CONVERSATION_ID, ME)).thenReturn(List.of(fromOther));

        messageService.markRead(ME, CONVERSATION_ID);

        assertTrue(fromOther.isRead());
        assertNotNull(fromOther.getReadAt());
        verify(messageRepository).saveAll(any());
    }

    @Test
    void markRead_isIdempotentWhenNothingUnread() {
        when(conversationRepository.findByIdWithParticipants(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findUnreadFromOther(CONVERSATION_ID, ME)).thenReturn(List.of());

        messageService.markRead(ME, CONVERSATION_ID);

        verify(messageRepository, never()).saveAll(any());
    }

    @Test
    void markRead_byNonParticipant_throwsNotFound() {
        Conversation foreign = conversation(200L, match(user(3L, "Eve"), other, pet(31L, null, "Nix"), otherPet, MatchStatus.MATCHED));
        when(conversationRepository.findByIdWithParticipants(200L)).thenReturn(Optional.of(foreign));

        assertThrows(ConversationNotFoundException.class, () -> messageService.markRead(ME, 200L));
        verify(messageRepository, never()).findUnreadFromOther(any(), any());
    }

    private User user(Long id, String firstName) {
        return User.builder().id(id).firstName(firstName).lastName("Doe")
                .email(firstName.toLowerCase() + "@example.com")
                .profilePicture("https://cdn/" + id + ".png").build();
    }

    private Pet pet(Long id, User owner, String name) {
        return Pet.builder().id(id).name(name).owner(owner).active(true).build();
    }

    private Match match(User user1, User user2, Pet pet1, Pet pet2, MatchStatus status) {
        return Match.builder().id(MATCH_ID).status(status).user1(user1).user2(user2)
                .pet1(pet1).pet2(pet2).build();
    }

    private Conversation conversation(Long id, Match match) {
        return Conversation.builder().id(id).match(match).build();
    }

    private Message message(Long id, User sender, String content, LocalDateTime sentAt, boolean read) {
        return Message.builder().id(id).content(content).read(read).sentAt(sentAt)
                .conversation(conversation).sender(sender).build();
    }

    private PetPhoto photo(Long id, Pet pet) {
        return PetPhoto.builder().id(id).url("https://cdn/" + pet.getName().toLowerCase() + ".jpg")
                .primaryPhoto(true).displayOrder(0).pet(pet).build();
    }
}