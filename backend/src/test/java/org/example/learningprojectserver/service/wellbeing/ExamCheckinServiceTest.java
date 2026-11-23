package org.example.learningprojectserver.service.wellbeing;

import org.example.learningprojectserver.dto.WellbeingDTOs.CheckinRequest;
import org.example.learningprojectserver.dto.WellbeingDTOs.CheckinResult;
import org.example.learningprojectserver.entities.StudentMoodCheckinEntity;
import org.example.learningprojectserver.entities.StudentWellbeingConsentEntity;
import org.example.learningprojectserver.mappers.StudentMoodCheckinEntityToDTOMapper;
import org.example.learningprojectserver.notification.dto.NotificationDTO;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.StudentMoodCheckinRepository;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.repository.StudentWellbeingConsentRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamCheckinServiceTest {

    @Mock StudentMoodCheckinRepository checkinRepository;
    @Mock StudentWellbeingConsentRepository consentRepository;
    @Mock StudentRepository studentRepository;
    @Mock ReflectionService reflectionService;
    @Mock NotificationEventPublisher publisher;
    ExamCheckinService service;

    @BeforeEach
    void setUp() {
        service = new ExamCheckinService(checkinRepository, consentRepository, studentRepository,
                new StudentMoodCheckinEntityToDTOMapper(), reflectionService, publisher);
        lenient().when(studentRepository.findAllTestsForStudent("s1")).thenReturn(List.of());
        lenient().when(checkinRepository.findFirstByStudentUserIdAndCheckinDate(eq("s1"), any())).thenReturn(Optional.empty());
    }

    private static StudentMoodCheckinEntity row(boolean distress) {
        StudentMoodCheckinEntity c = new StudentMoodCheckinEntity();
        c.setStudentUserId("s1");
        c.setCheckinDate(LocalDate.now());
        c.setMood(distress ? "STRESSED" : "OK");
        c.setDistress(distress);
        return c;
    }

    private void history(boolean... distress) {
        List<StudentMoodCheckinEntity> rows = new ArrayList<>();
        for (boolean d : distress) rows.add(row(d));
        when(checkinRepository.findTop10ByStudentUserIdOrderByCheckinDateDescIdDesc("s1")).thenReturn(rows);
    }

    private void consent(boolean consented) {
        StudentWellbeingConsentEntity e = new StudentWellbeingConsentEntity("s1");
        e.setConsented(consented);
        when(consentRepository.findById("s1")).thenReturn(Optional.of(e));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void distressPatternWithConsentAlertsTheSchoolManagerWithoutTheEntries() {
        history(true, true, false, true, false);
        consent(true);
        when(checkinRepository.existsByStudentUserIdAndEscalatedTrueAndCheckinDateAfter(eq("s1"), any())).thenReturn(false);
        when(checkinRepository.findStudentContact("s1")).thenReturn(List.<Object[]>of(new Object[]{"דנה", "ח1", "mgr"}));

        BasicResponse<CheckinResult> res = service.checkIn("s1", new CheckinRequest("OVERWHELMED"));

        assertTrue(res.isSuccess());
        assertTrue(res.getData().alertSent());
        assertEquals("BREATHING", res.getData().suggestion().kind());
        ArgumentCaptor<Collection> to = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<NotificationDTO> dto = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(publisher).publish(to.capture(), dto.capture());
        assertEquals(List.of("mgr"), new ArrayList<>(to.getValue()));
        assertTrue(dto.getValue().getBody().contains("דנה"));
        assertFalse(dto.getValue().getBody().contains("OVERWHELMED"));
    }

    @Test
    void noConsentMeansNoAlert() {
        history(true, true, true);
        consent(false);
        when(checkinRepository.existsByStudentUserIdAndEscalatedTrueAndCheckinDateAfter(eq("s1"), any())).thenReturn(false);

        BasicResponse<CheckinResult> res = service.checkIn("s1", new CheckinRequest("STRESSED"));

        assertTrue(res.getData().distressPattern());
        assertFalse(res.getData().alertSent());
        verifyNoInteractions(publisher);
    }

    @Test
    void noRepeatedAlertWithinTheCooldown() {
        history(true, true, true);
        consent(true);
        when(checkinRepository.existsByStudentUserIdAndEscalatedTrueAndCheckinDateAfter(eq("s1"), any())).thenReturn(true);

        BasicResponse<CheckinResult> res = service.checkIn("s1", new CheckinRequest("STRESSED"));

        assertFalse(res.getData().alertSent());
        verifyNoInteractions(publisher);
    }

    @Test
    void unknownMoodIsRejected() {
        BasicResponse<CheckinResult> res = service.checkIn("s1", new CheckinRequest("HAPPY"));
        assertFalse(res.isSuccess());
        verify(checkinRepository, never()).save(any());
    }

    @Test
    void consentIsStoredWithTimestampAndRevocable() {
        when(consentRepository.findById("s1")).thenReturn(Optional.empty());
        var given = service.setConsent("s1", true);
        assertTrue(given.getData().consented());
        assertNotNull(given.getData().consentedAt());
        assertEquals(WellbeingPolicy.PRIVACY_POLICY_VERSION, given.getData().policyVersion());

        StudentWellbeingConsentEntity stored = new StudentWellbeingConsentEntity("s1");
        stored.setConsented(true);
        when(consentRepository.findById("s1")).thenReturn(Optional.of(stored));
        var revoked = service.setConsent("s1", false);
        assertFalse(revoked.getData().consented());
        assertNotNull(revoked.getData().revokedAt());
    }
}
