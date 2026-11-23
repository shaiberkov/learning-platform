package org.example.learningprojectserver.service;

import org.example.learningprojectserver.dto.StudyRoomDTOs.Phase;
import org.example.learningprojectserver.dto.StudyRoomDTOs.RoomState;
import org.example.learningprojectserver.entities.StudentEntity;
import org.example.learningprojectserver.entities.StudyRoomEntity;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.repository.StudyRoomRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyRoomServiceTest {

    @Mock StudyRoomRepository roomRepository;
    @Mock StudentRepository studentRepository;
    @InjectMocks StudyRoomService service;

    private static final long MIN = 60_000L;

    private StudyRoomEntity room(long id, String school, LocalDateTime timerStart) {
        StudyRoomEntity r = new StudyRoomEntity();
        r.setId(id);
        r.setSchoolCode(school);
        r.setName("חדר מתמטיקה");
        r.setCreatedByUserId("s1");
        r.setCreatedAt(timerStart);
        r.setTimerStartedAt(timerStart);
        r.setLastActivityAt(StudyRoomService.now());
        r.setActive(true);
        return r;
    }

    private void student(String userId, String school, String name) {
        lenient().when(studentRepository.findSchoolCodeByUserId(userId)).thenReturn(school);
        StudentEntity s = new StudentEntity();
        s.setUsername(name);
        lenient().when(studentRepository.findStudentByStudentId(userId)).thenReturn(s);
    }

    @Test
    void phaseCyclesTwentyFiveMinutesFocusThenFiveBreak() {
        Phase start = StudyRoomService.phaseAt(0, 0);
        assertEquals("FOCUS", start.phase());
        assertEquals(25 * 60, start.secondsLeft());

        Phase lateFocus = StudyRoomService.phaseAt(0, 24 * MIN + 30_000);
        assertEquals("FOCUS", lateFocus.phase());
        assertEquals(30, lateFocus.secondsLeft());

        Phase brk = StudyRoomService.phaseAt(0, 26 * MIN);
        assertEquals("BREAK", brk.phase());
        assertEquals(4 * 60, brk.secondsLeft());
        assertEquals(25 * MIN, brk.phaseStartMs());

        Phase nextCycle = StudyRoomService.phaseAt(0, 31 * MIN);
        assertEquals("FOCUS", nextCycle.phase());
        assertEquals(24 * 60, nextCycle.secondsLeft());
        assertEquals(30 * MIN, nextCycle.phaseStartMs());
    }

    @Test
    void heartbeatShowsWhoIsInTheRoomAndWhatTheyWorkOn() {
        StudyRoomEntity r = room(7, "S1", StudyRoomService.now());
        when(roomRepository.findById(7L)).thenReturn(Optional.of(r));
        student("noa", "S1", "נועה");
        student("dan", "S1", "דן");

        service.heartbeat("noa", 7L, "משוואות");
        BasicResponse<RoomState> res = service.heartbeat("dan", 7L, null);

        assertTrue(res.isSuccess());
        List<String> names = res.getData().members().stream().map(m -> m.name() + ":" + m.task()).toList();
        assertTrue(names.contains("נועה:משוואות"));
        assertTrue(names.contains("דן:null"));
        assertTrue(res.getData().members().stream().filter(m -> m.name().equals("דן")).allMatch(m -> m.you()));
        assertEquals("FOCUS", res.getData().phase());
        assertFalse(res.getData().chatOpen());
    }

    @Test
    void studentOfAnotherSchoolCannotJoin() {
        StudyRoomEntity r = room(7, "S1", StudyRoomService.now());
        when(roomRepository.findById(7L)).thenReturn(Optional.of(r));
        student("eve", "S2", "איב");

        BasicResponse<RoomState> res = service.heartbeat("eve", 7L, null);

        assertFalse(res.isSuccess());
        assertNull(res.getData());
    }

    @Test
    void chatIsClosedDuringFocusAndOpenInTheBreak() {
        StudyRoomEntity focusRoom = room(1, "S1", StudyRoomService.now().minusMinutes(3));
        StudyRoomEntity breakRoom = room(2, "S1", StudyRoomService.now().minusMinutes(27));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(focusRoom));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(breakRoom));
        student("noa", "S1", "נועה");

        service.heartbeat("noa", 1L, null);
        assertFalse(service.sendChat("noa", 1L, "היי").isSuccess());

        service.heartbeat("noa", 2L, null);
        BasicResponse<RoomState> res = service.sendChat("noa", 2L, "היי, איך הולך?");
        assertTrue(res.isSuccess());
        assertTrue(res.getData().chatOpen());
        assertEquals(1, res.getData().chat().size());
        assertEquals("היי, איך הולך?", res.getData().chat().get(0).text());
    }

    @Test
    void chatNeedsPresenceInTheRoom() {
        StudyRoomEntity breakRoom = room(2, "S1", StudyRoomService.now().minusMinutes(27));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(breakRoom));
        student("noa", "S1", "נועה");

        assertFalse(service.sendChat("noa", 2L, "היי").isSuccess());
    }

    @Test
    void createRoomValidatesNameAndStartsTheTimer() {
        student("noa", "S1", "נועה");
        assertFalse(service.createRoom("noa", " ").isSuccess());

        when(roomRepository.save(any())).thenAnswer(inv -> {
            StudyRoomEntity r = inv.getArgument(0);
            r.setId(5L);
            return r;
        });
        when(roomRepository.findById(5L)).thenAnswer(inv -> Optional.of(room(5, "S1", StudyRoomService.now())));

        BasicResponse<RoomState> res = service.createRoom("noa", "  לומדים לבגרות ");
        assertTrue(res.isSuccess());
        verify(roomRepository).save(argThat(r -> r.getName().equals("לומדים לבגרות") && r.getSchoolCode().equals("S1")));
        assertEquals(1, res.getData().members().size());
    }

    @Test
    void studentWithoutSchoolGetsAHebrewError() {
        when(studentRepository.findSchoolCodeByUserId("x")).thenReturn(null);
        BasicResponse<?> res = service.listRooms("x");
        assertFalse(res.isSuccess());
        assertNotNull(res.getErrorCode());
    }

    @Test
    void idleEmptyRoomsAreClosedWhenListed() {
        StudyRoomEntity idle = room(3, "S1", StudyRoomService.now().minusHours(5));
        idle.setLastActivityAt(StudyRoomService.now().minusHours(3));
        StudyRoomEntity fresh = room(4, "S1", StudyRoomService.now());
        when(studentRepository.findSchoolCodeByUserId("noa")).thenReturn("S1");
        when(roomRepository.findBySchoolCodeAndActiveTrueOrderByCreatedAtDesc("S1")).thenReturn(List.of(idle, fresh));

        var res = service.listRooms("noa");

        assertEquals(1, res.getData().size());
        assertEquals(4L, res.getData().get(0).id());
        assertFalse(idle.isActive());
        verify(roomRepository).save(idle);
    }
}
