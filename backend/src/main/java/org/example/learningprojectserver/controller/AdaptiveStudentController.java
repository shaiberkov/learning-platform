package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.ExamFeedbackService;
import org.example.learningprojectserver.service.HintService;
import org.example.learningprojectserver.service.MasteryService;
import org.example.learningprojectserver.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.example.learningprojectserver.constants.ControllerConstants.Adaptive.*;

/** Student side of adaptive learning. Like every route group, open to all callers (see SecurityConfig). */
@RestController
@RequestMapping(STUDENT_ADAPTIVE_BASE_PATH)
@RequiredArgsConstructor
public class AdaptiveStudentController {

    private final ReviewService reviewService;
    private final MasteryService masteryService;
    private final HintService hintService;
    private final ExamFeedbackService examFeedbackService;

    @GetMapping(REVIEW_DUE)
    public BasicResponse getDueReviewItems(@RequestParam String userId) {
        return reviewService.getDueItems(userId);
    }

    @GetMapping(REVIEW_SUMMARY)
    public BasicResponse getReviewSummary(@RequestParam String userId) {
        return reviewService.getSummary(userId);
    }

    @PostMapping(REVIEW_ANSWER)
    public BasicResponse answerReviewItem(@RequestParam String userId,
                                          @RequestParam Long itemId,
                                          @RequestParam String answer) {
        return reviewService.answer(userId, itemId, answer);
    }

    @GetMapping(SKILL_MAP)
    public BasicResponse getSkillMap(@RequestParam String userId) {
        return masteryService.getSkillMap(userId);
    }

    @GetMapping(PRACTICE_HINT)
    public BasicResponse getPracticeHint(@RequestParam String userId,
                                         @RequestParam Long questionId,
                                         @RequestParam int step) {
        return hintService.getHintStep(userId, questionId, step);
    }

    @PostMapping(EXAM_QUESTION_TIMES)
    public BasicResponse recordExamQuestionTimes(@RequestParam String userId,
                                                 @RequestParam Long testId,
                                                 @RequestBody Map<Long, Integer> secondsByQuestion) {
        return examFeedbackService.recordQuestionTimes(userId, testId, secondsByQuestion);
    }

    @GetMapping(EXAM_FEEDBACK)
    public BasicResponse getExamFeedback(@RequestParam String userId, @RequestParam Long testId) {
        return examFeedbackService.getFeedback(userId, testId);
    }
}
