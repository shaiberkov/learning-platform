package org.example.learningprojectserver.service.wellbeing;

import org.example.learningprojectserver.dto.WellbeingDTOs.Reflection;
import org.example.learningprojectserver.dto.WellbeingDTOs.ReflectionRequest;
import org.example.learningprojectserver.entities.StudentDailyActivityEntity;
import org.example.learningprojectserver.entities.StudentReflectionEntity;
import org.example.learningprojectserver.mappers.StudentReflectionEntityToDTOMapper;
import org.example.learningprojectserver.notification.dto.NotificationDTO;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.StudentDailyActivityRepository;
import org.example.learningprojectserver.repository.StudentReflectionRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.EngagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReflectionServiceTest {

    @Mock StudentReflectionRepository reflectionRepository;
    @Mock StudentDailyActivityRepository dailyActivityRepository;
    @Mock NotificationEventPublisher publisher;
    ReflectionService service;

    @BeforeEach
    void setUp() {
        service = new ReflectionService(reflectionRepository, dailyActivityRepository,
                new StudentReflectionEntityToDTOMapper(), publisher);
    }

    private static StudentReflectionEntity reflection(String type, Integer target, LocalDate week) {
        StudentReflectionEntity r = new StudentReflectionEntity();
        r.setId(7L);
        r.setStudentUserId("s1");
        r.setWeekStart(week);
        r.setNextGoal("goal");
        r.setGoalType(type);
        r.setGoalTarget(target);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    @Test
    void savesThisWeeksReflectionForTheCurrentWeek() {
        LocalDate week = WellbeingPolicy.weekStart(EngagementService.today());
        when(reflectionRepository.findByStudentUserIdAndWeekStart("s1", week)).thenReturn(Optional.empty());
        when(reflectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BasicResponse<Reflection> res = service.save("s1",
                new ReflectionRequest(" שברים ", "", "20 שאלות", "PRACTICE_QUESTIONS", 20));

        assertTrue(res.isSuccess());
        ArgumentCaptor<StudentReflectionEntity> saved = ArgumentCaptor.forClass(StudentReflectionEntity.class);
        verify(reflectionRepository).save(saved.capture());
        assertEquals(week, saved.getValue().getWeekStart());
        assertEquals("שברים", saved.getValue().getWentWell());
        assertNull(saved.getValue().getStuckOn());
        assertEquals(20, saved.getValue().getGoalTarget());
        assertEquals("IN_PROGRESS", res.getData().status());
    }

    @Test
    void measurableGoalNeedsATarget() {
        BasicResponse<Reflection> res = service.save("s1", new ReflectionRequest(null, null, "goal", "XP", null));
        assertFalse(res.isSuccess());
        verify(reflectionRepository, never()).save(any());
    }

    @Test
    void xpGoalIsCheckedFromDailyActivity() {
        LocalDate lastWeek = WellbeingPolicy.weekStart(EngagementService.today()).minusWeeks(1);
        StudentReflectionEntity r = reflection("XP", 100, lastWeek);
        StudentDailyActivityEntity a = new StudentDailyActivityEntity("s1", lastWeek);
        a.setXp(60);
        StudentDailyActivityEntity b = new StudentDailyActivityEntity("s1", lastWeek.plusDays(2));
        b.setXp(50);
        when(dailyActivityRepository.findByStudentUserIdAndActivityDateBetweenOrderByActivityDate("s1", lastWeek, lastWeek.plusDays(6)))
                .thenReturn(List.of(a, b));

        Reflection dto = service.withStatus(r, EngagementService.today());

        assertEquals(110, dto.progress());
        assertEquals("ACHIEVED", dto.status());
    }

    @Test
    void measurableGoalCannotBeMarkedByHand() {
        when(reflectionRepository.findByIdAndStudentUserId(7L, "s1"))
                .thenReturn(Optional.of(reflection("PRACTICE_QUESTIONS", 10, LocalDate.now())));
        assertFalse(service.markAchieved("s1", 7L, true).isSuccess());
        verify(reflectionRepository, never()).save(any());
    }

    @Test
    void anotherStudentsReflectionIsNotFound() {
        when(reflectionRepository.findByIdAndStudentUserId(7L, "s2")).thenReturn(Optional.empty());
        assertFalse(service.markAchieved("s2", 7L, true).isSuccess());
    }

    @Test
    void freeGoalIsMarkedByTheStudent() {
        LocalDate week = WellbeingPolicy.weekStart(EngagementService.today());
        when(reflectionRepository.findByIdAndStudentUserId(7L, "s1")).thenReturn(Optional.of(reflection("FREE", null, week)));

        BasicResponse<Reflection> res = service.markAchieved("s1", 7L, true);

        assertTrue(res.isSuccess());
        assertEquals("ACHIEVED", res.getData().status());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void sundayReminderGoesToStudentsWithoutAReflection() {
        when(reflectionRepository.findStudentsWithoutReflection(any())).thenReturn(List.of("a", "b"));

        assertEquals(2, service.sendSundayReminders());

        ArgumentCaptor<NotificationDTO> dto = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(publisher).publish(eq(List.of("a", "b")), dto.capture());
        assertEquals("/reflection", dto.getValue().getLink());
    }
}
