package org.example.learningprojectserver.service;

import org.example.learningprojectserver.dto.StudyNoteDTOs.StudyNoteDTO;
import org.example.learningprojectserver.dto.StudyNoteDTOs.StudyNoteRequest;
import org.example.learningprojectserver.entities.StudyNoteEntity;
import org.example.learningprojectserver.mappers.StudyNoteEntityToDTOMapper;
import org.example.learningprojectserver.repository.StudyNoteRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyNoteServiceTest {

    private static final String ME = "student-1";

    @Mock StudyNoteRepository noteRepository;
    @Spy StudyNoteEntityToDTOMapper mapper = new StudyNoteEntityToDTOMapper();
    @InjectMocks StudyNoteService service;

    private static StudyNoteRequest note(String kind, String targetType, Long targetId, String quote, String body) {
        return new StudyNoteRequest(kind, targetType, targetId, "2x+3=7", "מתמטיקה", "משוואות", "משוואה ממעלה ראשונה",
                quote, body);
    }

    private static StudyNoteEntity stored(long id, String owner) {
        StudyNoteEntity n = new StudyNoteEntity();
        n.setId(id);
        n.setStudentUserId(owner);
        n.setKind("NOTE");
        n.setTargetType("QUESTION");
        n.setTargetId(4L);
        n.setBody("ישן");
        n.setCreatedAt(LocalDateTime.now());
        return n;
    }

    @Test
    void createsANoteForTheCaller() {
        when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BasicResponse<StudyNoteDTO> response = service.create(ME,
                note("NOTE", "QUESTION", 4L, null, "  פה טעיתי כי שכחתי להפוך סימן "));

        assertTrue(response.isSuccess());
        assertEquals("פה טעיתי כי שכחתי להפוך סימן", response.getData().body());
        assertEquals("משוואות", response.getData().topic());
        verify(noteRepository).save(argThat(n -> n.getStudentUserId().equals(ME) && n.getCreatedAt() != null));
    }

    @Test
    void rejectsInvalidRequests() {
        assertNotNull(StudyNoteService.validate(note("SCRIBBLE", "QUESTION", 4L, null, "x")));
        assertNotNull(StudyNoteService.validate(note("NOTE", "SCHOOL", 4L, null, "x")));
        assertNotNull(StudyNoteService.validate(note("NOTE", "QUESTION", null, null, "x")));
        assertNotNull(StudyNoteService.validate(note("NOTE", "QUESTION", 4L, null, "  ")));
        assertNotNull(StudyNoteService.validate(note("HIGHLIGHT", "LESSON", 4L, null, null)));
        assertNull(StudyNoteService.validate(note("HIGHLIGHT", "LESSON", 4L, "הקטע", null)));
        assertNull(StudyNoteService.validate(note("BOOKMARK", "LESSON", 4L, null, null)));

        BasicResponse<StudyNoteDTO> response = service.create(ME, note("NOTE", "QUESTION", 4L, null, ""));
        assertFalse(response.isSuccess());
        verify(noteRepository, never()).save(any());
    }

    @Test
    void listPassesTheFiltersAndTreatsBlankAsNone() {
        when(noteRepository.search(ME, "משוואות", null, null)).thenReturn(List.of(stored(1, ME)));

        List<StudyNoteDTO> notes = service.list(ME, "משוואות", " ", null).getData();

        assertEquals(1, notes.size());
        verify(noteRepository).search(ME, "משוואות", null, null);
    }

    @Test
    void cannotEditOrDeleteSomeoneElsesNote() {
        when(noteRepository.findByIdAndStudentUserId(9L, ME)).thenReturn(Optional.empty());

        assertFalse(service.update(ME, 9L, note("NOTE", "QUESTION", 4L, null, "חדש")).isSuccess());
        assertFalse(service.delete(ME, 9L).isSuccess());
        verify(noteRepository, never()).save(any());
        verify(noteRepository, never()).delete(any());
    }

    @Test
    void updateChangesOnlyTheText() {
        StudyNoteEntity existing = stored(9, ME);
        when(noteRepository.findByIdAndStudentUserId(9L, ME)).thenReturn(Optional.of(existing));
        when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BasicResponse<StudyNoteDTO> response = service.update(ME, 9L, note("BOOKMARK", "LESSON", 77L, null, "חדש"));

        assertTrue(response.isSuccess());
        assertEquals("חדש", existing.getBody());
        assertEquals("NOTE", existing.getKind());
        assertEquals("QUESTION", existing.getTargetType());
        assertEquals(4L, existing.getTargetId());
        assertNotNull(existing.getUpdatedAt());
    }
}
