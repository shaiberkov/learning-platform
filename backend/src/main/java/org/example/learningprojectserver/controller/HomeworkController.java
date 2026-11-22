package org.example.learningprojectserver.controller;

import org.springframework.security.access.prepost.PreAuthorize;

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

    @PreAuthorize("@authz.isSelf(#teacherId)")
    @GetMapping(TEACHER_CLASSES)
    public BasicResponse getTeacherClasses(@RequestParam String teacherId) {
        return homeworkService.getTeacherClasses(teacherId);
    }

    @PreAuthorize("@authz.isSelf(#teacherId)")
    @PostMapping(CREATE)
    public BasicResponse create(@RequestParam String teacherId, @RequestBody HomeworkRequest request) {
        return homeworkService.create(teacherId, request);
    }

    @PreAuthorize("@authz.isSelf(#teacherId)")
    @GetMapping(TEACHER_BOARD)
    public BasicResponse getTeacherBoard(@RequestParam String teacherId) {
        return homeworkService.getTeacherBoard(teacherId);
    }

    @PreAuthorize("@authz.isSelf(#studentId)")
    @GetMapping(STUDENT_HOMEWORK)
    public BasicResponse getStudentHomework(@RequestParam String studentId) {
        return homeworkService.getStudentHomework(studentId);
    }

    /** Body: {"content": "..."} */
    @PreAuthorize("@authz.isSelf(#studentId)")
    @PostMapping(SUBMIT)
    public BasicResponse submit(@RequestParam String studentId, @RequestParam Long homeworkId,
                                @RequestBody Map<String, String> body) {
        return homeworkService.submit(studentId, homeworkId, body == null ? null : body.get("content"));
    }
}
