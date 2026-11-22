package org.example.learningprojectserver.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.LessonDTO;
import org.example.learningprojectserver.dto.UserDto;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.learningprojectserver.constants.ControllerConstants.SchoolManager.*;

@RestController
@RequestMapping(SCHOOL_MANAGER_BASE_PATH)
@RequiredArgsConstructor
public class SchoolManagerController {
private final ClassRoomService classRoomService;
    private final SchoolGradeService schoolGradeService;
    private final SchoolManagerService schoolManagerService;
    private final SchoolService schoolService;
    private final TeacherService teacherService;


    @PreAuthorize("@authz.canAccessSchool(#schoolCode)")
    @GetMapping(GET_TEACHER_DTO)
    public BasicResponse getTeacherDTO(@RequestParam String teacherId,@RequestParam String schoolCode) {
        return teacherService.getTeacherDTO(teacherId,schoolCode);
    }

    @PreAuthorize("@authz.canAccessSchool(#schoolCode)")
    @GetMapping(GET_ALL_CLASSES_BY_SCHOOL_CODE)
    public BasicResponse getAllClassesNameBySchoolCode(@RequestParam String schoolCode) {
        return schoolService.getAllClassesNameBySchoolCode(schoolCode);
    }

    @PreAuthorize("@authz.isSelf(#userId)")
    @GetMapping(GET_SCHOOL_CODE)
    public BasicResponse getSchoolCode(@RequestParam String userId){
        return schoolManagerService.getSchoolCode(userId);

    }

@PreAuthorize("@authz.canAccessSchool(#schoolCode)")
@GetMapping(GET_SCHEDULE_FOR_CLASSROOM)
public BasicResponse getAllLessonsForClassRoom(@RequestParam String schoolCode , @RequestParam String classRoomName) {
        return classRoomService.getScheduleForClassRoom(schoolCode, classRoomName);

}

    @PreAuthorize("@authz.canAccessSchool(#schoolCode)")
    @PostMapping(ASSIGN_USER_AS_SCHOOL_TEACHER)
    public BasicResponse assignUserAsSchoolTeacher(@RequestParam String userId, @RequestParam String schoolCode) {
        return schoolManagerService.assignUserAsSchoolTeacher(userId, schoolCode);
    }

    @PreAuthorize("@authz.canManageUser(#userId)")
    @PostMapping(REMOVE_TEACHER_FROM_SCHOOL)
    public BasicResponse removeTeacherFromSchool(@RequestParam String userId) {
        return schoolManagerService.removeTeacherFromSchool(userId);
    }

    @PreAuthorize("@authz.canAccessSchool(#schoolCode)")
    @PostMapping(ADD_SCHOOL_GRADES)
    public BasicResponse addSchoolGrades(@RequestParam String schoolCode, @RequestBody List<String> gradesName) {
        return schoolManagerService.addSchoolGrades(schoolCode, gradesName);
    }

    @PreAuthorize("@authz.canAccessSchool(#schoolCode)")
    @PostMapping(REMOVE_SCHOOL_GRADES)
    public BasicResponse removeSchoolGrades(@RequestParam String schoolCode, @RequestBody List<String> gradesName) {
        return schoolManagerService.removeSchoolGrades(schoolCode, gradesName);
    }

    @PreAuthorize("@authz.canAccessSchool(#schoolCode)")
    @PostMapping(ADD_CLASSES_TO_GRADE)
    public BasicResponse addClassesToGrade(
            @RequestParam String schoolCode,
            @RequestParam String gradeName,
            @RequestParam int classesCount) {
        return schoolManagerService.addClassesToGrade(schoolCode, gradeName, classesCount);

    }

    @PreAuthorize("@authz.canAccessSchool(#schoolCode)")
    @PostMapping(ADD_ADDITIONAL_CLASS_TO_GRADE)
    public BasicResponse addAdditionalClassToGrade(
            @RequestParam String schoolCode,
            @RequestParam String gradeName,
            @RequestParam String className) {

        return schoolManagerService.addAdditionalClassToGrade(schoolCode, gradeName, className);

    }

    @PreAuthorize("@authz.canAccessSchool(#schoolCode) and @authz.canManageUser(#teacherId)")
    @PostMapping(ASSIGN_TEACHER_TO_CLASSES)
    public BasicResponse assignTeacherToClasses(
            @RequestParam String schoolCode,
            @RequestParam String teacherId,
            @RequestBody List<String> classNames) {
        return schoolManagerService.assignTeacherToClasses(schoolCode, teacherId, classNames);
    }

    @PreAuthorize("@authz.canAccessSchool(#schoolCode) and @authz.canManageUser(#teacherId)")
    @PostMapping(REMOVE_TEACHER_FROM_CLASS)
    public BasicResponse removeTeacherFromClass(@RequestParam String schoolCode,
                                                @RequestParam String teacherId,
                                                @RequestParam String className) {
        return schoolManagerService.removeTeacherFromClass(schoolCode, teacherId, className);
    }

    @PreAuthorize("@authz.canAccessSchool(#schoolCode)")
    @GetMapping(GET_CLASS_NAMES_BY_SCHOOL_AND_GRADE)
    public BasicResponse getClassNamesBySchoolAndGrade(@RequestParam String schoolCode, @RequestParam String gradeName) {
        return schoolGradeService.getClassNamesBySchoolAndGrade(schoolCode, gradeName);
    }

    @PreAuthorize("@authz.canAccessSchool(#schoolCode)")
    @GetMapping(GET_GRADES)
    public BasicResponse getGradesBySchoolCode(@RequestParam String schoolCode) {
        return schoolService.getGradesBySchoolCode(schoolCode);
    }

    @PreAuthorize("@authz.canManageUser(#teacherId)")
    @PostMapping(ADD_LESSON_TO_TEACHER)
    public BasicResponse addLessonToTeacher(@RequestBody LessonDTO dto, @RequestParam String teacherId) {
        return teacherService.addLessonToTeacher(dto, teacherId);

    }

    @PreAuthorize("@authz.canManageUser(#teacherId)")
    @PostMapping(ADD_TEACHING_SUBJECT_TO_TEACHER)
    public BasicResponse addTeachingSubjectToTeacher(@RequestParam String teacherId, @RequestBody List<String> subjects) {
        return teacherService.addTeachingSubjectToTeacher(teacherId, subjects);

    }

    @PreAuthorize("@authz.canManageUser(#teacherId)")
    @PostMapping(REMOVE_TEACHING_SUBJECT_FROM_TEACHER)
    public BasicResponse removeTeachingSubjectFromTeacher(@RequestParam String teacherId, @RequestParam String subjectToRemove) {
        return teacherService.removeTeachingSubjectFromTeacher(teacherId, subjectToRemove);
    }

    @PreAuthorize("@authz.canAccessSchool(#schoolCode)")
    @PostMapping(ASSIGN_STUDENT_TO_CLASS)
    public BasicResponse assignStudentToClass(
            @RequestParam String schoolCode,
            @RequestParam String studentId,
            @RequestParam String className
    ) {
        return schoolManagerService.assignStudentToClass(schoolCode, studentId, className);
    }

    @PreAuthorize("@authz.canAccessSchool(#schoolCode) and @authz.canManageUser(#studentId)")
    @PostMapping(CHANGE_STUDENT_CLASS)
    public BasicResponse changeStudentClass(
            @RequestParam String schoolCode,
            @RequestParam String studentId,
            @RequestParam String gradeName,
            @RequestParam String newClassName
    ) {
        return schoolManagerService.changeStudentClass(schoolCode, studentId, gradeName, newClassName);
    }
    @PreAuthorize("@authz.canAccessSchool(#schoolCode)")
    @GetMapping(GET_ALL_TEACHERS)
    public BasicResponse getAllTeachers(@RequestParam String schoolCode) {
        return schoolManagerService.getAllTeachers(schoolCode);
    }

    @PreAuthorize("@authz.canAccessSchool(#schoolCode)")
    @GetMapping(GET_ALL_STUDENTS)
    public BasicResponse getAllStudents(@RequestParam String schoolCode) {
        return schoolManagerService.getAllStudents(schoolCode);
    }

    @PreAuthorize("@authz.canManageUser(#teacherId)")
    @GetMapping(GET_TEACHER_TEACHING_SUBJECTS)
    public BasicResponse getTeacherTeachingSubjects(@RequestParam String teacherId) {
        return teacherService.getTeacherTeachingSubjects(teacherId);
    }

    @PreAuthorize("@authz.isSelf(#schoolManagerId)")
    @GetMapping(GET_SCHOOL)
    public BasicResponse getSchool(@RequestParam String schoolManagerId) {
        return schoolService.getSchoolDTO(schoolManagerId);
    }
}







