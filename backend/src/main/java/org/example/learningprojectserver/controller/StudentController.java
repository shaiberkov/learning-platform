package org.example.learningprojectserver.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.StudentTestStatusDTO;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.StudentService;
import org.example.learningprojectserver.service.TestResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.example.learningprojectserver.constants.ControllerConstants.Student.*;

@RestController
@RequestMapping(STUDENT_BASE_PATH)
@RequiredArgsConstructor
public class StudentController {

private final StudentService studentService;
private final TestResultService testResultService;



    @PreAuthorize("@authz.isSelf(#studentId) and @authz.canAccessSchool(#schoolCode)")
    @GetMapping(GET_STUDENT_SCHEDULE)
    public BasicResponse getStudentSchedule(@RequestParam String schoolCode,@RequestParam String studentId){
        return studentService.getStudentSchedule(schoolCode,studentId);
    }

    @PreAuthorize("@authz.isSelf(#userId)")
    @GetMapping(GENERATE_QUESTION)
    public BasicResponse generateQuestionForPractice(
            @RequestParam String userId,
            @RequestParam String subject,
            @RequestParam String topic,
            @RequestParam String subTopic) {
        return studentService.generateQuestionForPractice(userId, subject, topic, subTopic);
    }

    @PreAuthorize("@authz.isSelf(#userId)")
    @PostMapping(SUBMIT_ANSWER)
    public BasicResponse submitAnswer(
            @RequestParam String userId,
            @RequestParam Long id,
            @RequestParam String subTopic,
            @RequestParam String answer) {
        return studentService.submitAnswer(userId,id,subTopic,answer);
    }
    @PreAuthorize("@authz.isSelf(#userId) and @features.enabledForCurrentUser('PRACTICE_TESTS')")
    @PostMapping(GENERATE_PRACTICE_TEST)
    public BasicResponse generatePracticeTest(
            @RequestParam String userId,
            @RequestParam String subject,
            @RequestParam String topic,
            @RequestParam String difficulty,
            @Min(value = 1, message = "מספר השאלות חייב להיות לפחות 1") @Max(value = 100, message = "מספר השאלות לא יכול לעלות על 100") @RequestParam int questionCount,
            @Min(value = 1, message = "זמן המבחן חייב להיות לפחות דקה אחת") @RequestParam int timeLimitMinutes) {

        return studentService.generatePracticeTest(userId, subject, topic, difficulty, questionCount, timeLimitMinutes);
    }

    @PreAuthorize("@authz.ownsPracticeTest(#userId, #testId)")
    @PostMapping(CHECK_PRACTICE_TEST)
    public BasicResponse checkPracticeTest(
            @RequestParam String userId,
            @RequestParam Long testId,
            @RequestBody Map<Long, String> userAnswers) {

        return testResultService.checkPracticeTest(userId, testId, userAnswers);
    }



    @PreAuthorize("@authz.isAssignedToTeacherTest(#userId, #testId)")
    @PostMapping(START_TEST)
    public BasicResponse startTest(
            @RequestParam String userId,
            @RequestParam Long testId) {
        return testResultService.startTest(testId,userId);
    }

    @PreAuthorize("@authz.isSelf(#studentId)")
    @GetMapping(GET_STUDENT_TESTS_STATUS)
    public List<StudentTestStatusDTO> getStudentTestsStatus(@RequestParam String studentId){
        return studentService.getStudentTestsStatus(studentId);
    }


}
