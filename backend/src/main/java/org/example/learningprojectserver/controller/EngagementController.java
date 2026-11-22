package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.ClassChallengeRequest;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.EngagementService;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.Engagement.*;

@RestController
@RequestMapping(ENGAGEMENT_BASE_PATH)
@RequiredArgsConstructor
public class EngagementController {

    private final EngagementService engagementService;

    @GetMapping(GET_SUMMARY)
    public BasicResponse getSummary(@RequestParam String studentId) {
        return engagementService.getSummary(studentId);
    }

    @PostMapping(CREATE_CLASS_CHALLENGE)
    public BasicResponse createClassChallenge(@RequestParam String teacherId, @RequestBody ClassChallengeRequest request) {
        return engagementService.createClassChallenge(teacherId, request);
    }

    @GetMapping(TEACHER_CHALLENGES)
    public BasicResponse getTeacherChallenges(@RequestParam String teacherId) {
        return engagementService.getTeacherChallenges(teacherId);
    }
}
