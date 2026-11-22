package org.example.learningprojectserver.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.AccommodationService;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.Accommodation.*;

@RestController
@RequestMapping(ACCOMMODATION_BASE_PATH)
@RequiredArgsConstructor
public class AccommodationController {

    private final AccommodationService accommodationService;

    @PreAuthorize("@authz.isSelf(#studentId)")
    @GetMapping(GET)
    public BasicResponse get(@RequestParam String studentId) {
        return accommodationService.get(studentId);
    }

    @PreAuthorize("@authz.isSelf(#teacherId)")
    @GetMapping(TEACHER_STUDENTS)
    public BasicResponse getTeacherStudents(@RequestParam String teacherId) {
        return accommodationService.getTeacherStudents(teacherId);
    }

    @PreAuthorize("@authz.isSelf(#teacherId)")
    @PostMapping(SET_EXTRA_TIME)
    public BasicResponse setExtraTime(@RequestParam String teacherId, @RequestParam String studentId,
                                      @RequestParam int percent) {
        return accommodationService.setExtraTime(teacherId, studentId, percent);
    }

    @PreAuthorize("@authz.isSelf(#studentId)")
    @PostMapping(SET_PREFERENCES)
    public BasicResponse setPreferences(@RequestParam String studentId, @RequestParam boolean readAloud,
                                        @RequestParam boolean dyslexiaFont) {
        return accommodationService.setPreferences(studentId, readAloud, dyslexiaFont);
    }
}
