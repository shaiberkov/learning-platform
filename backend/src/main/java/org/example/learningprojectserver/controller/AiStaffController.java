package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.ai.AiChatMessageDTO;
import org.example.learningprojectserver.dto.ai.AiQuestionDraftDTO;
import org.example.learningprojectserver.dto.ai.AiUsageByFeatureDTO;
import org.example.learningprojectserver.dto.ai.BankQuestionDTO;
import org.example.learningprojectserver.dto.ai.OpenAnswerDTO;
import org.example.learningprojectserver.dto.ai.OpenAnswerInputDTO;
import org.example.learningprojectserver.dto.ai.QuestionGenerationRequestDTO;
import org.example.learningprojectserver.dto.ai.RubricDTO;
import org.example.learningprojectserver.dto.ai.TeacherInsightDTO;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.ai.AiException;
import org.example.learningprojectserver.service.ai.AiOversightService;
import org.example.learningprojectserver.service.ai.AiUser;
import org.example.learningprojectserver.service.ai.AiUserResolver;
import org.example.learningprojectserver.service.ai.grading.AiGradingService;
import org.example.learningprojectserver.service.ai.insights.TeacherInsightService;
import org.example.learningprojectserver.service.ai.questions.AiQuestionGenerationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.learningprojectserver.constants.ControllerConstants.AiStaff.*;

/**
 * AI for teachers and school staff. {@code SecurityConfig} restricts this path to TEACHER,
 * SCHOOLMANAGER and SYSTEM_ADMIN; ownership (a teacher's own drafts, rubrics, insights) is checked
 * in the services against the user taken from the JWT.
 */
@RestController
@RequestMapping(AI_STAFF_BASE_PATH)
@RequiredArgsConstructor
public class AiStaffController {

    private final AiUserResolver userResolver;
    private final AiQuestionGenerationService questionService;
    private final AiGradingService gradingService;
    private final TeacherInsightService insightService;
    private final AiOversightService oversightService;

    private AiUser caller(Object principal) {
        return userResolver.resolve(AiUserResolver.userIdFromPrincipal(principal));
    }

    // ---- question generation ----

    @PostMapping(QUESTIONS_GENERATE)
    public BasicResponse<List<AiQuestionDraftDTO>> generate(@AuthenticationPrincipal Object principal,
                                                            @RequestBody QuestionGenerationRequestDTO request,
                                                            @RequestParam(defaultValue = "false") boolean fresh) {
        return questionService.generate(caller(principal), request, fresh);
    }

    @GetMapping(QUESTION_DRAFTS)
    public BasicResponse<List<AiQuestionDraftDTO>> drafts(@AuthenticationPrincipal Object principal) {
        return questionService.pendingDrafts(caller(principal));
    }

    @PutMapping(QUESTION_DRAFT)
    public BasicResponse<AiQuestionDraftDTO> updateDraft(@AuthenticationPrincipal Object principal,
                                                         @PathVariable Long draftId,
                                                         @RequestBody AiQuestionDraftDTO changes) {
        return questionService.updateDraft(caller(principal), draftId, changes);
    }

    @PostMapping(QUESTION_DRAFT_APPROVE)
    public BasicResponse<BankQuestionDTO> approveDraft(@AuthenticationPrincipal Object principal, @PathVariable Long draftId) {
        return questionService.approve(caller(principal), draftId);
    }

    @PostMapping(QUESTION_DRAFT_REJECT)
    public BasicResponse<Void> rejectDraft(@AuthenticationPrincipal Object principal, @PathVariable Long draftId) {
        return questionService.reject(caller(principal), draftId);
    }

    @GetMapping(QUESTION_BANK)
    public BasicResponse<List<BankQuestionDTO>> bank(@AuthenticationPrincipal Object principal) {
        return questionService.bank(caller(principal));
    }

    // ---- grading ----

    @GetMapping(RUBRICS)
    public BasicResponse<List<RubricDTO>> rubrics(@AuthenticationPrincipal Object principal) {
        return gradingService.teacherRubrics(caller(principal));
    }

    @PostMapping(RUBRICS)
    public BasicResponse<RubricDTO> createRubric(@AuthenticationPrincipal Object principal, @RequestBody RubricDTO rubric) {
        return gradingService.createRubric(caller(principal), rubric);
    }

    @GetMapping(RUBRIC_ANSWERS)
    public BasicResponse<List<OpenAnswerDTO>> answers(@AuthenticationPrincipal Object principal, @PathVariable Long rubricId) {
        return gradingService.answersForRubric(caller(principal), rubricId);
    }

    @PostMapping(RUBRIC_ANSWERS)
    public BasicResponse<OpenAnswerDTO> addAnswer(@AuthenticationPrincipal Object principal, @PathVariable Long rubricId,
                                                  @RequestBody OpenAnswerInputDTO input) {
        input.setRubricId(rubricId);
        return gradingService.addAnswerByTeacher(caller(principal), input);
    }

    @PostMapping(ANSWER_SUGGEST)
    public BasicResponse<OpenAnswerDTO> suggest(@AuthenticationPrincipal Object principal, @PathVariable Long answerId) {
        return gradingService.suggestScore(caller(principal), answerId);
    }

    @PostMapping(ANSWER_APPROVE)
    public BasicResponse<OpenAnswerDTO> approveAnswer(@AuthenticationPrincipal Object principal, @PathVariable Long answerId,
                                                      @RequestBody(required = false) OpenAnswerInputDTO decision) {
        return gradingService.approve(caller(principal), answerId, decision);
    }

    // ---- insights, usage, safety ----

    @GetMapping(INSIGHTS_LATEST)
    public BasicResponse<TeacherInsightDTO> latestInsight(@AuthenticationPrincipal Object principal) {
        return insightService.latest(caller(principal));
    }

    @PostMapping(INSIGHTS_GENERATE)
    public BasicResponse<TeacherInsightDTO> generateInsight(@AuthenticationPrincipal Object principal) {
        return insightService.generate(caller(principal));
    }

    @GetMapping(USAGE)
    public BasicResponse<List<AiUsageByFeatureDTO>> usage(@AuthenticationPrincipal Object principal,
                                                          @RequestParam(defaultValue = "30") int days) {
        return oversightService.schoolUsage(caller(principal), days);
    }

    @GetMapping(SAFETY_FLAGGED)
    public BasicResponse<List<AiChatMessageDTO>> flagged(@AuthenticationPrincipal Object principal) {
        return oversightService.flaggedMessages(caller(principal));
    }

    @ExceptionHandler(AiException.class)
    public BasicResponse<Void> handleAiException(AiException e) {
        return new BasicResponse<>(false, e.getMessage());
    }
}
