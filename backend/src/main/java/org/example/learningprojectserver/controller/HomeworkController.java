package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.HomeworkRequest;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.HomeworkService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.example.learningprojectserver.constants.ControllerConstants.Homework.*;

@RestController
@RequestMapping(HOMEWORK_BASE_PATH)
@RequiredArgsConstructor
public class HomeworkController {

    private final HomeworkService homeworkService;

    @GetMapping(TEACHER_CLASSES)
    public BasicResponse getTeacherClasses(@RequestParam String teacherId) {
        return homeworkService.getTeacherClasses(teacherId);
    }

    @PostMapping(CREATE)
    public BasicResponse create(@RequestParam String teacherId, @RequestBody HomeworkRequest request) {
        return homeworkService.create(teacherId, request);
    }

    @GetMapping(TEACHER_BOARD)
    public BasicResponse getTeacherBoard(@RequestParam String teacherId) {
        return homeworkService.getTeacherBoard(teacherId);
    }

    @GetMapping(STUDENT_HOMEWORK)
    public BasicResponse getStudentHomework(@RequestParam String studentId) {
        return homeworkService.getStudentHomework(studentId);
    }

    /** Body: {"content": "..."} */
    @PostMapping(SUBMIT)
    public BasicResponse submit(@RequestParam String studentId, @RequestParam Long homeworkId,
                                @RequestBody Map<String, String> body) {
        return homeworkService.submit(studentId, homeworkId, body == null ? null : body.get("content"));
    }
}
