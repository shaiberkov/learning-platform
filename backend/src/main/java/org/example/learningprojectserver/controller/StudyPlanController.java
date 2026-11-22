package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.StudyPlanService;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.StudyPlan.*;

@RestController
@RequestMapping(STUDY_PLAN_BASE_PATH)
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @GetMapping(GET_PLAN)
    public BasicResponse getPlan(@RequestParam String studentId) {
        return studyPlanService.getPlan(studentId);
    }

    @PostMapping(COMPLETE_TASK)
    public BasicResponse completeTask(@RequestParam String studentId, @RequestParam Long taskId) {
        return studyPlanService.completeTask(studentId, taskId);
    }
}
