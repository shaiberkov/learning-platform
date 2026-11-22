package org.example.learningprojectserver.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.TeacherService;
import org.example.learningprojectserver.service.TestResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.example.learningprojectserver.constants.ControllerConstants.Teacher.*;

@RestController
@RequestMapping(TEACHER_BASE_PATH)
@RequiredArgsConstructor
public class TeacherController {

private final TeacherService teacherService;
private final TestResultService testResultService;


    @PreAuthorize("@authz.isSelf(#teacherId)")
    @GetMapping(SCHEDULE_FOR_TEACHER)
    public BasicResponse getScheduleForTeacher(@RequestParam String teacherId) {
        return teacherService.getTeacherSchedule(teacherId);
    }


    @PreAuthorize("@authz.teachesStudents(#teacherId, #usersIds) and @features.enabledForCurrentUser('TEACHER_TESTS')")
    @PostMapping(GENERATE_TEST_FOR_STUDENTS)
    public BasicResponse generateTestForStudents(
            @RequestBody List<String> usersIds,
            @RequestParam String teacherId,
            @RequestParam String testStartTime,
            @RequestParam String subject,
            @RequestParam String topic,
            @RequestParam String difficulty,
            @RequestParam int questionCount,
            @RequestParam int timeLimitMinutes) {

            return teacherService.generateTestForStudents(
                    usersIds,
                    teacherId,
                    testStartTime,
                    subject,
                    topic,
                    difficulty,
                    questionCount,
                    timeLimitMinutes
            );
        }
    @PreAuthorize("@authz.isAssignedToTeacherTest(#userId, #testId)")
    @PostMapping(CHECK_TEACHER_TEST)
    public BasicResponse checkTeacherTest(
            @RequestParam String userId,
            @RequestParam Long testId,
            @RequestBody Map<Long, String> userAnswers) {
        return testResultService.checkTeacherTest(userId, testId, userAnswers);
    }

}
