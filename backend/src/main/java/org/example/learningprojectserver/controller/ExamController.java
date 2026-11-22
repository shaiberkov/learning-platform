package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.exam.*;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.ExamAttemptService;
import org.example.learningprojectserver.service.ExamAuthoringService;
import org.example.learningprojectserver.service.ItemAnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.example.learningprojectserver.constants.ControllerConstants.Exam.*;

/**
 * Secure exam flow (student) and exam management (teacher).
 * Like every route group, these are permitAll() in SecurityConfig: the userId / teacherId
 * parameters are trusted, so ownership checks in the services are only as strong as that.
 */
@RestController
@RequestMapping(EXAM_BASE_PATH)
@RequiredArgsConstructor
public class ExamController {

    private final ExamAttemptService examAttemptService;
    private final ExamAuthoringService examAuthoringService;
    private final ItemAnalysisService itemAnalysisService;

    // ---- student ----

    @PostMapping(START_ATTEMPT)
    public BasicResponse<ExamAttemptView> startAttempt(@RequestParam String userId, @RequestParam Long testId) {
        return examAttemptService.startAttempt(userId, testId);
    }

    @PostMapping(SAVE_ANSWERS)
    public BasicResponse<Long> saveAnswers(@RequestParam String userId, @RequestParam Long attemptId,
                                           @RequestBody Map<Long, String> answers) {
        return examAttemptService.saveAnswers(userId, attemptId, answers);
    }

    @PostMapping(SUBMIT_ATTEMPT)
    public BasicResponse<ExamResultView> submitAttempt(@RequestParam String userId, @RequestParam Long attemptId,
                                                       @RequestBody Map<Long, String> answers) {
        return examAttemptService.submitAttempt(userId, attemptId, answers);
    }

    @PostMapping(INTEGRITY_EVENT)
    public BasicResponse<Integer> integrityEvent(@RequestParam String userId, @RequestParam Long attemptId,
                                                 @RequestParam String type) {
        return examAttemptService.recordIntegrityEvent(userId, attemptId, type);
    }

    // ---- teacher ----

    @PostMapping(CREATE_EXAM)
    public BasicResponse<List<TeacherExamSummary>> createExam(@RequestParam String teacherId,
                                                              @RequestBody CreateExamRequest request) {
        return examAuthoringService.createExam(teacherId, request);
    }

    @PostMapping(CLONE_EXAM)
    public BasicResponse<List<TeacherExamSummary>> cloneExam(@RequestParam String teacherId, @RequestParam Long testId,
                                                             @RequestBody CloneExamRequest request) {
        return examAuthoringService.cloneExam(teacherId, testId, request);
    }

    @GetMapping(TEACHER_EXAMS)
    public BasicResponse<List<TeacherExamSummary>> teacherExams(@RequestParam String teacherId) {
        return examAuthoringService.listTeacherExams(teacherId);
    }

    @GetMapping(EXAM_ATTEMPTS)
    public BasicResponse<List<AttemptMonitorView>> attempts(@RequestParam String teacherId, @RequestParam Long testId) {
        return examAttemptService.getAttemptsForTeacher(teacherId, testId);
    }

    @GetMapping(ITEM_ANALYSIS)
    public BasicResponse<ItemAnalysisView> itemAnalysis(@RequestParam String teacherId, @RequestParam Long testId) {
        return itemAnalysisService.analyse(teacherId, testId);
    }
}
