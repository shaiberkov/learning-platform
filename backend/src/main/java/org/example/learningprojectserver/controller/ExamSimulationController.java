package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.examsim.ExamSimulationService;
import org.example.learningprojectserver.service.gradebook.GradeTargetService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.example.learningprojectserver.constants.ControllerConstants.ExamSimulation.*;

/**
 * Student side of past-exam simulations and the grade-target calculator, under /student (STUDENT and
 * SYSTEM_ADMIN, see SecurityConfig). The acting student is always the authenticated user; attempt ids
 * are checked against that user in the service.
 */
@RestController
@RequestMapping(EXAM_SIM_BASE_PATH)
@RequiredArgsConstructor
public class ExamSimulationController {

    private final ExamSimulationService examSimulationService;
    private final GradeTargetService gradeTargetService;

    @GetMapping(SIM_EXAMS)
    public BasicResponse<?> listExams() {
        return examSimulationService.listExams(CurrentUser.userId());
    }

    @GetMapping(SIM_PREDICTION)
    public BasicResponse<?> prediction(@RequestParam Long examId) {
        return examSimulationService.predict(CurrentUser.userId(), examId);
    }

    @PostMapping(SIM_START)
    public BasicResponse<?> start(@RequestParam Long examId) {
        return examSimulationService.start(CurrentUser.userId(), examId);
    }

    @PostMapping(SIM_SAVE)
    public BasicResponse<?> save(@RequestParam Long attemptId, @RequestBody(required = false) Map<Long, String> answers) {
        return examSimulationService.save(CurrentUser.userId(), attemptId, answers);
    }

    @PostMapping(SIM_SUBMIT)
    public BasicResponse<?> submit(@RequestParam Long attemptId, @RequestBody(required = false) Map<Long, String> answers) {
        return examSimulationService.submit(CurrentUser.userId(), attemptId, answers);
    }

    @GetMapping(SIM_RESULT)
    public BasicResponse<?> result(@RequestParam Long attemptId) {
        return examSimulationService.result(CurrentUser.userId(), attemptId);
    }

    @GetMapping(GRADE_TARGET)
    public BasicResponse<?> gradeTarget(@RequestParam Double target) {
        return gradeTargetService.calculate(CurrentUser.userId(), target);
    }
}
