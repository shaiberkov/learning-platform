package org.example.learningprojectserver.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.TeacherService;
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


    @PreAuthorize("@authz.isSelf(#teacherId)")
    @GetMapping(SCHEDULE_FOR_TEACHER)
    public BasicResponse getScheduleForTeacher(@RequestParam String teacherId) {
        return teacherService.getTeacherSchedule(teacherId);
    }


    @PreAuthorize("@authz.teachesStudents(#teacherId, #usersIds) and @features.enabledForCurrentUser('TEACHER_TESTS')")
    @PostMapping(GENERATE_TEST_FOR_STUDENTS)
    public BasicResponse generateTestForStudents(
            @NotEmpty(message = "יש לבחור לפחות תלמיד אחד") @RequestBody List<String> usersIds,
            @RequestParam String teacherId,
            @RequestParam String testStartTime,
            @RequestParam String subject,
            @RequestParam String topic,
            @RequestParam String difficulty,
            @Min(value = 1, message = "מספר השאלות חייב להיות לפחות 1") @Max(value = 100, message = "מספר השאלות לא יכול לעלות על 100") @RequestParam int questionCount,
            @Min(value = 1, message = "זמן המבחן חייב להיות לפחות דקה אחת") @RequestParam int timeLimitMinutes) {

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
    /**
     * Retired: it graded without the server timer. Teacher tests are submitted through
     * the exam attempt flow (ExamController.SUBMIT_ATTEMPT), which enforces the deadline.
     */
    @PreAuthorize("@authz.isAssignedToTeacherTest(#userId, #testId)")
    @PostMapping(CHECK_TEACHER_TEST)
    public BasicResponse checkTeacherTest(
            @RequestParam String userId,
            @RequestParam Long testId,
            @RequestBody Map<Long, String> userAnswers) {
        return new BasicResponse(false, "הגשת מבחן מתבצעת דרך מסך המבחן");
    }

}
