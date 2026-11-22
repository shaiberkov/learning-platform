package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.GradePromotionDTO;
import org.example.learningprojectserver.dto.TimetableLessonDTO;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.learningprojectserver.constants.ControllerConstants.SchoolAdministration.*;

/**
 * School manager tools: bulk import, dashboard analytics, timetable builder, school years, parent links.
 * Lives under the school-manager path, which SecurityConfig leaves permitAll (roles are not enforced).
 */
@RestController
@RequestMapping(SCHOOL_ADMINISTRATION_BASE_PATH)
@RequiredArgsConstructor
public class SchoolAdministrationController {

    private final BulkImportService bulkImportService;
    private final SchoolAnalyticsService schoolAnalyticsService;
    private final TimetableService timetableService;
    private final SchoolYearService schoolYearService;
    private final ParentService parentService;

    /** Body: the CSV file's text (UTF-8). */
    @PostMapping(value = BULK_IMPORT_PREVIEW, consumes = {"text/plain", "text/csv"})
    public BasicResponse previewBulkImport(@RequestParam String schoolCode, @RequestBody String csv) {
        return bulkImportService.preview(schoolCode, csv);
    }

    @PostMapping(value = BULK_IMPORT_COMMIT, consumes = {"text/plain", "text/csv"})
    public BasicResponse commitBulkImport(@RequestParam String schoolCode, @RequestBody String csv) {
        return bulkImportService.commit(schoolCode, csv);
    }

    @GetMapping(SCHOOL_ANALYTICS)
    public BasicResponse getSchoolAnalytics(@RequestParam String schoolCode) {
        return schoolAnalyticsService.getSchoolAnalytics(schoolCode);
    }

    @GetMapping(TIMETABLE_TEACHERS)
    public BasicResponse getTimetableTeachers(@RequestParam String schoolCode) {
        return timetableService.getTeacherOptions(schoolCode);
    }

    @GetMapping(TIMETABLE_LESSONS)
    public BasicResponse getTimetableLessons(@RequestParam String schoolCode) {
        return timetableService.getSchoolTimetable(schoolCode);
    }

    @PostMapping(TIMETABLE_CHECK)
    public BasicResponse checkTimetableLesson(@RequestParam String schoolCode, @RequestBody TimetableLessonDTO lesson) {
        return timetableService.checkLesson(schoolCode, lesson);
    }

    @PostMapping(TIMETABLE_SAVE)
    public BasicResponse saveTimetableLesson(@RequestParam String schoolCode, @RequestBody TimetableLessonDTO lesson) {
        return timetableService.saveLesson(schoolCode, lesson);
    }

    @PostMapping(TIMETABLE_DELETE)
    public BasicResponse deleteTimetableLesson(@RequestParam String schoolCode, @RequestParam Long lessonId) {
        return timetableService.deleteLesson(schoolCode, lessonId);
    }

    @GetMapping(SCHOOL_YEARS)
    public BasicResponse getSchoolYears(@RequestParam String schoolCode) {
        return schoolYearService.getYears(schoolCode);
    }

    @PostMapping(CREATE_SCHOOL_YEAR)
    public BasicResponse createSchoolYear(@RequestParam String schoolCode, @RequestParam String name,
                                          @RequestParam String startDate, @RequestParam String endDate) {
        return schoolYearService.createYear(schoolCode, name, startDate, endDate);
    }

    @PostMapping(ADD_SEMESTER)
    public BasicResponse addSemester(@RequestParam String schoolCode, @RequestParam Long yearId, @RequestParam String name,
                                     @RequestParam String startDate, @RequestParam String endDate) {
        return schoolYearService.addSemester(schoolCode, yearId, name, startDate, endDate);
    }

    @PostMapping(REMOVE_SEMESTER)
    public BasicResponse removeSemester(@RequestParam String schoolCode, @RequestParam Long yearId, @RequestParam Long semesterId) {
        return schoolYearService.removeSemester(schoolCode, yearId, semesterId);
    }

    @GetMapping(PROMOTION_PLAN)
    public BasicResponse getPromotionPlan(@RequestParam String schoolCode) {
        return schoolYearService.getPromotionPlan(schoolCode);
    }

    /** Body: the promotion plan; an empty list uses the default plan. */
    @PostMapping(CLOSE_SCHOOL_YEAR)
    public BasicResponse closeSchoolYear(@RequestParam String schoolCode, @RequestParam Long yearId,
                                         @RequestParam(required = false) String nextYearName,
                                         @RequestBody(required = false) List<GradePromotionDTO> plan) {
        return schoolYearService.closeYearAndPromote(schoolCode, yearId, plan, nextYearName);
    }

    @GetMapping(SCHOOL_YEAR_ARCHIVE)
    public BasicResponse getSchoolYearArchive(@RequestParam String schoolCode, @RequestParam Long yearId) {
        return schoolYearService.getArchive(schoolCode, yearId);
    }

    @PostMapping(LINK_PARENT)
    public BasicResponse linkParent(@RequestParam String schoolCode, @RequestParam String parentId, @RequestParam String studentId) {
        return parentService.linkParentToChild(schoolCode, parentId, studentId);
    }

    @PostMapping(UNLINK_PARENT)
    public BasicResponse unlinkParent(@RequestParam String schoolCode, @RequestParam String parentId, @RequestParam String studentId) {
        return parentService.unlinkParentFromChild(schoolCode, parentId, studentId);
    }
}
