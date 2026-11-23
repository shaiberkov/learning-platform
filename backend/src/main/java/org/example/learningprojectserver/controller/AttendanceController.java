package org.example.learningprojectserver.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.AttendanceMarkRequest;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.attendance.AttendanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.learningprojectserver.constants.ControllerConstants.Attendance.*;

@RestController
@RequestMapping(ATTENDANCE_BASE_PATH)
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /** date: yyyy-MM-dd, default today. */
    @PreAuthorize("@authz.isSelf(#teacherId)")
    @GetMapping(GET_ATTENDANCE_LESSONS)
    public BasicResponse getLessonsForDay(@RequestParam String teacherId, @RequestParam(required = false) String date) {
        return attendanceService.getLessonsForDay(teacherId, date);
    }

    @PreAuthorize("@authz.isSelf(#teacherId)")
    @GetMapping(GET_ATTENDANCE_SHEET)
    public BasicResponse getSheet(@RequestParam String teacherId, @RequestParam Long lessonId,
                                  @RequestParam(required = false) String date) {
        return attendanceService.getSheet(teacherId, lessonId, date);
    }

    @PreAuthorize("@authz.isSelf(#teacherId)")
    @PostMapping(MARK_ATTENDANCE)
    public BasicResponse mark(@RequestParam String teacherId, @RequestParam Long lessonId,
                              @RequestParam(required = false) String date,
                              @RequestBody List<AttendanceMarkRequest> marks) {
        return attendanceService.mark(teacherId, lessonId, date, marks);
    }

    /** userId: a teacher (own lessons) or a school manager (whole school). from/to: yyyy-MM-dd. */
    @PreAuthorize("@authz.isSelf(#userId)")
    @GetMapping(GET_ATTENDANCE_REPORT)
    public BasicResponse getReport(@RequestParam String userId,
                                   @RequestParam(required = false) String from,
                                   @RequestParam(required = false) String to,
                                   @RequestParam(required = false) String className) {
        return attendanceService.getReport(userId, from, to, className);
    }
}
