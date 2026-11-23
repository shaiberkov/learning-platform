package org.example.learningprojectserver;

import org.example.learningprojectserver.entities.MessageEntity;
import org.example.learningprojectserver.entities.StudentEntity;
import org.example.learningprojectserver.entities.TeacherEntity;
import org.example.learningprojectserver.entities.UserEntity;
import org.example.learningprojectserver.mappers.MessageEntityToMessageDTOMapper;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.MessageAttachmentRepository;
import org.example.learningprojectserver.repository.MessageRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.MessageService;
import org.example.learningprojectserver.strategy.message.MessageRecipientStrategyFactory;
import org.example.learningprojectserver.utils.LocalizedMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceThreadTest {

    @Mock UserRepository userRepository;
    @Mock MessageRepository messageRepository;
    @Mock MessageAttachmentRepository messageAttachmentRepository;
    @Mock MessageRecipientStrategyFactory strategyFactory;
    @Mock MessageEntityToMessageDTOMapper mapper;
    @Mock NotificationEventPublisher publisher;
    @Mock CacheManager cacheManager;
    @Mock LocalizedMessages messages;

    @InjectMocks MessageService messageService;

    private TeacherEntity teacher;
    private StudentEntity student;
    private MessageEntity original;

    @BeforeEach
    void setUp() {
        lenient().when(messages.get(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messages.get(anyString())).thenAnswer(inv -> inv.getArgument(0));
        teacher = new TeacherEntity();
        teacher.setUserId("T1");
        teacher.setUsername("המורה");
        student = new StudentEntity();
        student.setUserId("S1");
        student.setUsername("התלמיד");
        original = new MessageEntity();
        original.setId(10L);
        original.setSender(teacher);
        original.setReceiver(student);
        original.setTitle("שיעורי בית");
        original.setContent("...");
        original.setSentAt(LocalDateTime.now());
        original.setBatchId("batch-1");
        original.setThreadId("batch-1");
    }

    @Test
    void studentReplyGoesToTheSenderInTheSameThread() {
        when(messageRepository.findById(10L)).thenReturn(Optional.of(original));

        BasicResponse response = messageService.reply("S1", 10L, "סיימתי", null);

        assertTrue(response.isSuccess());
        ArgumentCaptor<MessageEntity> saved = ArgumentCaptor.forClass(MessageEntity.class);
        verify(messageRepository).save(saved.capture());
        MessageEntity reply = saved.getValue();
        assertSame(student, reply.getSender());
        assertSame(teacher, reply.getReceiver());
        assertEquals("batch-1", reply.getThreadId());
        assertEquals(10L, reply.getParentMessageId());
        verify(publisher).publish(eq(List.of("T1")), any());
    }

    @Test
    void strangersCannotReply() {
        when(messageRepository.findById(10L)).thenReturn(Optional.of(original));

        BasicResponse response = messageService.reply("X9", 10L, "hi", null);

        assertFalse(response.isSuccess());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void onlyTheReceiverMarksAMessageRead() {
        when(messageRepository.findById(10L)).thenReturn(Optional.of(original));

        assertFalse(messageService.markAsRead("T1", 10L).isSuccess());
        assertNull(original.getReadAt());

        assertTrue(messageService.markAsRead("S1", 10L).isSuccess());
        assertNotNull(original.getReadAt());
    }

    @Test
    void scheduledMessagesArePendingAndNotDelivered() {
        UserEntity sender = teacher;
        when(userRepository.findUserByUserId("T1")).thenReturn(sender);
        when(strategyFactory.getStrategy(anyString(), anyString())).thenReturn(id -> List.of(student));

        BasicResponse response = messageService.sendMessage("T1", "כיתה", "א1", "כותרת", "תוכן",
                LocalDateTime.now().plusDays(1), null);

        assertTrue(response.isSuccess());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MessageEntity>> saved = ArgumentCaptor.forClass(List.class);
        verify(messageRepository).saveAll(saved.capture());
        MessageEntity copy = saved.getValue().get(0);
        assertEquals(Boolean.TRUE, copy.getPending());
        assertNotNull(copy.getBatchId());
        assertEquals(copy.getBatchId(), copy.getThreadId());
        verifyNoInteractions(publisher);
    }
}
