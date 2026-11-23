package org.example.learningprojectserver.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.LessonContentRequest;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.LessonContentService;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.LessonContent.*;

@RestController
@RequestMapping(LESSON_CONTENT_BASE_PATH)
@RequiredArgsConstructor
public class LessonContentController {

    private final LessonContentService lessonContentService;

    @PreAuthorize("@authz.isSelf(#teacherId)")
    @PostMapping(CREATE)
    public BasicResponse create(@RequestParam String teacherId, @RequestBody LessonContentRequest request) {
        return lessonContentService.create(teacherId, request);
    }

    @PreAuthorize("@authz.isSelf(#teacherId)")
    @PutMapping(UPDATE)
    public BasicResponse update(@RequestParam String teacherId, @RequestParam Long lessonId,
                                @RequestBody LessonContentRequest request) {
        return lessonContentService.update(teacherId, lessonId, request);
    }

    @PreAuthorize("@authz.isSelf(#teacherId)")
    @DeleteMapping(DELETE)
    public BasicResponse delete(@RequestParam String teacherId, @RequestParam Long lessonId) {
        return lessonContentService.delete(teacherId, lessonId);
    }

    @PreAuthorize("@authz.isSelf(#teacherId)")
    @GetMapping(TEACHER_LESSONS)
    public BasicResponse getTeacherLessons(@RequestParam String teacherId) {
        return lessonContentService.getTeacherLessons(teacherId);
    }

    @PreAuthorize("@authz.isSelf(#studentId)")
    @GetMapping(STUDENT_LESSONS)
    public BasicResponse getStudentLessons(@RequestParam String studentId) {
        return lessonContentService.getStudentLessons(studentId);
    }

    @PreAuthorize("#userId == null or @authz.isSelf(#userId)")
    @GetMapping(GET)
    public BasicResponse getLesson(@RequestParam Long lessonId, @RequestParam(required = false) String userId) {
        return lessonContentService.getLesson(lessonId, userId);
    }

    @PreAuthorize("@authz.isSelf(#studentId)")
    @PostMapping(UPDATE_PROGRESS)
    public BasicResponse updateProgress(@RequestParam String studentId, @RequestParam Long lessonId,
                                        @RequestParam String stage) {
        return lessonContentService.updateProgress(studentId, lessonId, stage);
    }
}
