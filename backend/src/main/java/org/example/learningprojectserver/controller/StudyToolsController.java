package org.example.learningprojectserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.FlashcardDTOs.*;
import org.example.learningprojectserver.dto.NoteTopicDTO;
import org.example.learningprojectserver.dto.StudyNoteDTOs.StudyNoteDTO;
import org.example.learningprojectserver.dto.StudyNoteDTOs.StudyNoteRequest;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.StudyNoteService;
import org.example.learningprojectserver.service.ai.AiException;
import org.example.learningprojectserver.service.ai.AiUserResolver;
import org.example.learningprojectserver.service.flashcards.FlashcardGenerationService;
import org.example.learningprojectserver.service.flashcards.FlashcardService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.learningprojectserver.constants.ControllerConstants.StudyTools.*;

/**
 * [s04] Flashcards and the personal study notebook. Under /student/** (STUDENT, SYSTEM_ADMIN in
 * SecurityConfig). No endpoint takes a userId: the student is the authenticated caller, and every
 * card or note id is looked up within that student's own data in the services.
 */
@RestController
@RequestMapping(STUDY_TOOLS_BASE_PATH)
@RequiredArgsConstructor
public class StudyToolsController {

    private final FlashcardService flashcardService;
    private final FlashcardGenerationService generationService;
    private final StudyNoteService noteService;
    private final AiUserResolver aiUserResolver;

    private static String me() {
        String userId = CurrentUser.userId();
        if (userId == null) {
            throw new AccessDeniedException("יש להתחבר מחדש");
        }
        return userId;
    }

    /* ---------- Flashcards ---------- */

    @GetMapping(FLASHCARDS)
    public BasicResponse<List<FlashcardDTO>> myDeck() {
        return flashcardService.myDeck(me());
    }

    @GetMapping(FLASHCARDS_DUE)
    public BasicResponse<List<FlashcardDTO>> dueCards() {
        return flashcardService.dueCards(me());
    }

    @GetMapping(FLASHCARDS_SUMMARY)
    public BasicResponse<FlashcardSummaryDTO> summary() {
        return flashcardService.summary(me());
    }

    @GetMapping(FLASHCARDS_CLASS)
    public BasicResponse<List<FlashcardDTO>> classCards() {
        return flashcardService.classCards(me());
    }

    @PostMapping(FLASHCARDS_GENERATE)
    public BasicResponse<FlashcardDraftsDTO> generate(@Valid @RequestBody FlashcardGenerateRequest request) {
        try {
            return generationService.generate(aiUserResolver.resolve(me()), request);
        } catch (AiException e) {
            return new BasicResponse<>(false, e.getMessage());
        }
    }

    @PostMapping(FLASHCARDS)
    public BasicResponse<List<FlashcardDTO>> save(@Valid @RequestBody FlashcardSaveRequest request) {
        return flashcardService.save(me(), request);
    }

    @PostMapping(FLASHCARD_SHARE)
    public BasicResponse<FlashcardDTO> share(@PathVariable Long cardId, @RequestParam boolean shared) {
        return flashcardService.setShared(me(), cardId, shared);
    }

    @PostMapping(FLASHCARD_ADD_TO_DECK)
    public BasicResponse<FlashcardDTO> addToDeck(@PathVariable Long cardId) {
        return flashcardService.addToDeck(me(), cardId);
    }

    @PostMapping(FLASHCARD_REVIEW)
    public BasicResponse<FlashcardReviewResultDTO> review(@PathVariable Long cardId, @RequestParam boolean knew) {
        return flashcardService.review(me(), cardId, knew);
    }

    @DeleteMapping(FLASHCARD_BY_ID)
    public BasicResponse<Void> remove(@PathVariable Long cardId) {
        return flashcardService.remove(me(), cardId);
    }

    /* ---------- Study notebook ---------- */

    @GetMapping(NOTES)
    public BasicResponse<List<StudyNoteDTO>> notes(@RequestParam(required = false) String topic,
                                                   @RequestParam(required = false) String targetType,
                                                   @RequestParam(required = false) Long targetId) {
        return noteService.list(me(), topic, targetType, targetId);
    }

    @GetMapping(NOTES_TOPICS)
    public BasicResponse<List<NoteTopicDTO>> noteTopics() {
        return noteService.topics(me());
    }

    @PostMapping(NOTES)
    public BasicResponse<StudyNoteDTO> createNote(@Valid @RequestBody StudyNoteRequest request) {
        return noteService.create(me(), request);
    }

    @PutMapping(NOTE_BY_ID)
    public BasicResponse<StudyNoteDTO> updateNote(@PathVariable Long noteId, @Valid @RequestBody StudyNoteRequest request) {
        return noteService.update(me(), noteId, request);
    }

    @DeleteMapping(NOTE_BY_ID)
    public BasicResponse<Void> deleteNote(@PathVariable Long noteId) {
        return noteService.delete(me(), noteId);
    }
}
