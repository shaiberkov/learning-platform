package org.example.learningprojectserver.service;

import org.example.learningprojectserver.entities.HomeworkEntity;
import org.example.learningprojectserver.entities.HomeworkSubmissionEntity;
import org.example.learningprojectserver.notification.NotificationType;
import org.example.learningprojectserver.notification.dto.NotificationDTO;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.HomeworkRepository;
import org.example.learningprojectserver.repository.HomeworkSubmissionRepository;
import org.example.learningprojectserver.repository.StudentAccommodationRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeworkServiceTest {

    @Mock HomeworkRepository homeworkRepository;
    @Mock HomeworkSubmissionRepository submissionRepository;
    @Mock StudentAccommodationRepository studentLookupRepository;
    @Mock NotificationEventPublisher publisher;
    @Mock EngagementEvents engagementEvents;
    @InjectMocks HomeworkService service;

    private HomeworkEntity homework(long id, LocalDateTime due, String... students) {
        HomeworkEntity h = new HomeworkEntity();
        h.setId(id);
        h.setTitle("תרגיל " + id);
        h.setDueDate(due);
        h.setStudentUserIds(new ArrayList<>(List.of(students)));
        return h;
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void remindersGoOnlyToStudentsWhoHaveNotSubmitted() {
        HomeworkEntity h = homework(1L, LocalDateTime.now(EngagementService.ZONE).plusHours(5), "a", "b", "c");
        HomeworkSubmissionEntity sub = new HomeworkSubmissionEntity();
        sub.setHomework(h);
        sub.setStudentUserId("b");
        when(homeworkRepository.findDueForReminder(any(), any())).thenReturn(List.of(h));
        when(submissionRepository.findByHomeworkIdIn(any())).thenReturn(List.of(sub));

        int sent = service.sendDueReminders();

        assertEquals(2, sent);
        assertTrue(h.isReminderSent());
        ArgumentCaptor<Collection> recipients = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<NotificationDTO> dto = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(publisher).publish(recipients.capture(), dto.capture());
        assertEquals(List.of("a", "c"), List.copyOf(recipients.getValue()));
        assertEquals(NotificationType.HOMEWORK_REMINDER, dto.getValue().getType());
    }

    @Test
    void lateSubmissionIsAcceptedAndFlagged() {
        HomeworkEntity h = homework(2L, LocalDateTime.now(EngagementService.ZONE).minusHours(1), "a");
        when(homeworkRepository.findById(2L)).thenReturn(Optional.of(h));
        when(submissionRepository.findFirstByHomeworkIdAndStudentUserId(2L, "a")).thenReturn(Optional.empty());

        BasicResponse response = service.submit("a", 2L, "התשובה שלי");

        assertTrue(response.isSuccess());
        ArgumentCaptor<HomeworkSubmissionEntity> saved = ArgumentCaptor.forClass(HomeworkSubmissionEntity.class);
        verify(submissionRepository).save(saved.capture());
        assertTrue(saved.getValue().isLate());
        verify(engagementEvents).homeworkSubmitted(eq("a"), eq(true), anyLong());
    }

    @Test
    void studentNotAssignedCannotSubmit() {
        HomeworkEntity h = homework(3L, LocalDateTime.now(EngagementService.ZONE).plusDays(1), "a");
        when(homeworkRepository.findById(3L)).thenReturn(Optional.of(h));

        BasicResponse response = service.submit("intruder", 3L, "x");

        assertFalse(response.isSuccess());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void engagementFailureNeverEscapes() {
        EngagementService failing = mock(EngagementService.class);
        doThrow(new RuntimeException("db down")).when(failing).recordActivity(anyString(), anyInt(), any());
        EngagementEvents events = new EngagementEvents(failing);
        assertDoesNotThrow(() -> events.practiceAnswer("a", true, "חיבור שברים", 5));
    }
}
