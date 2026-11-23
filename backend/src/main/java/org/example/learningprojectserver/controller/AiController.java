package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.ai.AiChatMessageDTO;
import org.example.learningprojectserver.dto.ai.AiStatusDTO;
import org.example.learningprojectserver.dto.ai.OpenAnswerDTO;
import org.example.learningprojectserver.dto.ai.OpenAnswerInputDTO;
import org.example.learningprojectserver.dto.ai.RubricDTO;
import org.example.learningprojectserver.dto.ai.TutorRequestDTO;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.ai.AiException;
import org.example.learningprojectserver.service.ai.AiUser;
import org.example.learningprojectserver.service.ai.AiUserResolver;
import org.example.learningprojectserver.service.ai.grading.AiGradingService;
import org.example.learningprojectserver.service.ai.tutor.TutorService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

import static org.example.learningprojectserver.constants.ControllerConstants.Ai.*;

/**
 * AI for every signed-in user (mostly students): the tutor, its status and history, voice
 * transcription, and open questions. The caller is always taken from the JWT, never from a parameter.
 */
@RestController
@RequestMapping(AI_BASE_PATH)
@RequiredArgsConstructor
public class AiController {

    private static final long MAX_AUDIO_BYTES = 5L * 1024 * 1024;

    private final AiUserResolver userResolver;
    private final TutorService tutorService;
    private final AiGradingService gradingService;

    private AiUser caller(Object principal) {
        return userResolver.resolve(AiUserResolver.userIdFromPrincipal(principal));
    }

    @GetMapping(STATUS)
    public BasicResponse<AiStatusDTO> status(@AuthenticationPrincipal Object principal) {
        return tutorService.status(caller(principal));
    }

    /** Server-sent events: {@code delta} (text), {@code replace} (text), {@code done} (remaining), {@code error} (message). */
    @PostMapping(value = TUTOR_STREAM, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter tutorStream(@AuthenticationPrincipal Object principal, @RequestBody TutorRequestDTO request) {
        AiUser user;
        try {
            user = caller(principal);
        } catch (AiException e) {
            return TutorService.errorEmitter(e.getMessage());
        }
        return tutorService.streamReply(user, request);
    }

    @GetMapping(TUTOR_HISTORY)
    public BasicResponse<List<AiChatMessageDTO>> history(@AuthenticationPrincipal Object principal,
                                                         @RequestParam(required = false) Long questionId) {
        return tutorService.getHistory(caller(principal), questionId);
    }

    @DeleteMapping(TUTOR_HISTORY)
    public BasicResponse<Integer> clearHistory(@AuthenticationPrincipal Object principal) {
        return tutorService.clearHistory(caller(principal));
    }

    @PostMapping(value = TUTOR_TRANSCRIBE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BasicResponse<String> transcribe(@AuthenticationPrincipal Object principal,
                                            @RequestParam("audio") MultipartFile audio) throws IOException {
        if (audio.getSize() > MAX_AUDIO_BYTES) {
            return new BasicResponse<>(false, "ההקלטה ארוכה מדי. נסו הקלטה קצרה יותר.");
        }
        return tutorService.transcribe(caller(principal), audio.getBytes(), audio.getOriginalFilename());
    }

    @GetMapping(OPEN_QUESTIONS)
    public BasicResponse<List<RubricDTO>> openQuestions(@AuthenticationPrincipal Object principal) {
        return gradingService.openQuestionsForStudent(caller(principal));
    }

    @GetMapping(OPEN_QUESTION_ANSWERS)
    public BasicResponse<List<OpenAnswerDTO>> myAnswers(@AuthenticationPrincipal Object principal) {
        return gradingService.studentAnswers(caller(principal));
    }

    @PostMapping(OPEN_QUESTION_ANSWERS)
    public BasicResponse<OpenAnswerDTO> submitAnswer(@AuthenticationPrincipal Object principal,
                                                     @RequestBody OpenAnswerInputDTO input) {
        return gradingService.submitByStudent(caller(principal), input);
    }

    @ExceptionHandler(AiException.class)
    public BasicResponse<Void> handleAiException(AiException e) {
        return new BasicResponse<>(false, e.getMessage());
    }
}
