package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.NoteTopicDTO;
import org.example.learningprojectserver.dto.StudyNoteDTOs.StudyNoteDTO;
import org.example.learningprojectserver.dto.StudyNoteDTOs.StudyNoteRequest;
import org.example.learningprojectserver.entities.StudyNoteEntity;
import org.example.learningprojectserver.mappers.StudyNoteEntityToDTOMapper;
import org.example.learningprojectserver.repository.StudyNoteRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * The student's personal notebook: notes, highlights and bookmarks attached to a question, a lesson,
 * a mistakes-notebook item or a flashcard. Before a test the student filters all of them by topic.
 * Notes are private: every read and write is scoped to the authenticated student.
 */
@Service
@RequiredArgsConstructor
public class StudyNoteService {

    static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Jerusalem");
    static final Set<String> KINDS = Set.of("NOTE", "HIGHLIGHT", "BOOKMARK");
    static final Set<String> TARGET_TYPES = Set.of("QUESTION", "LESSON", "REVIEW_ITEM", "FLASHCARD");

    static final String NOTE_NOT_FOUND = "ההערה לא נמצאה";

    private final StudyNoteRepository noteRepository;
    private final StudyNoteEntityToDTOMapper mapper;

    /** The student's notes, newest first; topic, targetType and targetId are optional filters. */
    public BasicResponse<List<StudyNoteDTO>> list(String userId, String topic, String targetType, Long targetId) {
        List<StudyNoteDTO> notes = noteRepository.search(userId, blankToNull(topic), blankToNull(targetType), targetId)
                .stream().map(mapper).toList();
        return ok(null, notes);
    }

    public BasicResponse<List<NoteTopicDTO>> topics(String userId) {
        return ok(null, noteRepository.countByTopic(userId));
    }

    public BasicResponse<StudyNoteDTO> create(String userId, StudyNoteRequest request) {
        String error = validate(request);
        if (error != null) {
            return new BasicResponse<>(false, error);
        }
        StudyNoteEntity note = new StudyNoteEntity();
        note.setStudentUserId(userId);
        note.setKind(request.kind());
        note.setTargetType(request.targetType());
        note.setTargetId(request.targetId());
        note.setTargetTitle(blankToNull(request.targetTitle()));
        note.setSubject(blankToNull(request.subject()));
        note.setTopic(blankToNull(request.topic()));
        note.setSubTopic(blankToNull(request.subTopic()));
        note.setQuote(blankToNull(request.quote()));
        note.setBody(blankToNull(request.body()));
        note.setCreatedAt(LocalDateTime.now(SCHOOL_ZONE));
        return ok(savedMessage(note.getKind()), mapper.apply(noteRepository.save(note)));
    }

    /** Changes the text of a note (and its highlighted passage); the target stays the same. */
    public BasicResponse<StudyNoteDTO> update(String userId, Long noteId, StudyNoteRequest request) {
        StudyNoteEntity note = noteRepository.findByIdAndStudentUserId(noteId, userId).orElse(null);
        if (note == null) {
            return new BasicResponse<>(false, NOTE_NOT_FOUND);
        }
        String body = blankToNull(request.body());
        String quote = blankToNull(request.quote());
        if ("NOTE".equals(note.getKind()) && body == null) {
            return new BasicResponse<>(false, "כתבו משהו בהערה");
        }
        note.setBody(body);
        note.setQuote(quote);
        note.setUpdatedAt(LocalDateTime.now(SCHOOL_ZONE));
        return ok("ההערה עודכנה", mapper.apply(noteRepository.save(note)));
    }

    public BasicResponse<Void> delete(String userId, Long noteId) {
        StudyNoteEntity note = noteRepository.findByIdAndStudentUserId(noteId, userId).orElse(null);
        if (note == null) {
            return new BasicResponse<>(false, NOTE_NOT_FOUND);
        }
        noteRepository.delete(note);
        return new BasicResponse<>(true, "ההערה נמחקה");
    }

    /** Hebrew error for an invalid request, or null. */
    static String validate(StudyNoteRequest request) {
        if (!KINDS.contains(request.kind())) {
            return "סוג ההערה לא מוכר";
        }
        if (!TARGET_TYPES.contains(request.targetType())) {
            return "אפשר לרשום הערה רק על שאלה, שיעור או כרטיסייה";
        }
        if (request.targetId() == null) {
            return "חסר למה ההערה שייכת";
        }
        if ("NOTE".equals(request.kind()) && blankToNull(request.body()) == null) {
            return "כתבו משהו בהערה";
        }
        if ("HIGHLIGHT".equals(request.kind()) && blankToNull(request.quote()) == null) {
            return "סמנו את הקטע שרוצים להדגיש";
        }
        return null;
    }

    private static String savedMessage(String kind) {
        return switch (kind) {
            case "HIGHLIGHT" -> "ההדגשה נשמרה במחברת";
            case "BOOKMARK" -> "נוספה סימנייה";
            default -> "ההערה נשמרה במחברת";
        };
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static <T> BasicResponse<T> ok(String message, T data) {
        BasicResponse<T> response = new BasicResponse<>(true, message);
        response.setData(data);
        return response;
    }
}
