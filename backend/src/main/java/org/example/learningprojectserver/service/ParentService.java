package org.example.learningprojectserver.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.AnalyticsBucketDTO;
import org.example.learningprojectserver.dto.ChildOverviewDTO;
import org.example.learningprojectserver.dto.ChildSummaryDTO;
import org.example.learningprojectserver.dto.GradeRecordDTO;
import org.example.learningprojectserver.entities.ParentEntity;
import org.example.learningprojectserver.entities.StudentEntity;
import org.example.learningprojectserver.entities.UserEntity;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.mappers.UserToParentMapper;
import org.example.learningprojectserver.repository.AttendanceRecordRepository;
import org.example.learningprojectserver.repository.ParentRepository;
import org.example.learningprojectserver.repository.SchoolAnalyticsRepository;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * The PARENT role: a manager links a parent to a child of the school; the parent sees each
 * child's grades and progress. The API does not enforce roles (see SecurityConfig), so every
 * read checks that the parent is linked to the child.
 */
@Service
@RequiredArgsConstructor
public class ParentService {

    private static final DateTimeFormatter TEST_START_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final SchoolAnalyticsRepository schoolAnalyticsRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserToParentMapper userToParentMapper;

    /**
     * Links a parent to a student of the manager's school. The parent is an existing PARENT user,
     * or a registered user with no school (role STUDENT, not in any class) who becomes a PARENT.
     */
    @Transactional
    public BasicResponse linkParentToChild(String schoolCode, String parentUserId, String childUserId) {
        StudentEntity child = studentRepository.findStudentByStudentId(childUserId);
        if (child == null || child.getSchoolName() == null || !schoolCode.equals(child.getSchoolName().getSchoolCode())) {
            return new BasicResponse(false, "התלמיד לא נמצא בבית הספר");
        }
        if (childUserId.equals(parentUserId)) {
            return new BasicResponse(false, "תלמיד לא יכול להיות ההורה של עצמו");
        }
        UserEntity user = userRepository.findUserByUserId(parentUserId);
        if (user == null) {
            return new BasicResponse(false, "המשתמש של ההורה לא נמצא. על ההורה להירשם קודם למערכת");
        }

        ParentEntity parent;
        if (user.getRole() == Role.PARENT) {
            parent = (ParentEntity) user;
        } else if (user.getRole() == Role.STUDENT && user instanceof StudentEntity s
                && s.getSchoolName() == null && s.getClassRoom() == null) {
            // Converting deletes and recreates the user, so refuse when rows outside the student's own
            // cascade (teachers, assigned teacher tests, a parent link) still point at it.
            if (!s.getTeachers().isEmpty() || !s.getTeacherTests().isEmpty() || !s.getTeacherTestsResult().isEmpty()
                    || parentRepository.isLinkedAsChild(s.getUserId())) {
                return new BasicResponse(false, "למשתמש " + user.getUsername()
                        + " יש היסטוריה כתלמיד (מורים, מבחנים או הורה מקושר) ולכן לא ניתן להפוך אותו להורה");
            }
            parent = userToParentMapper.apply(user);
            userRepository.delete(user);
            userRepository.flush();
        } else {
            return new BasicResponse(false, "המשתמש " + user.getUsername() + " משויך כבר כ־" + user.getRole() + " ולא ניתן להפוך אותו להורה");
        }

        if (parent.getChildren().stream().anyMatch(c -> c.getUserId().equals(childUserId))) {
            return new BasicResponse(false, "ההורה כבר מקושר לתלמיד זה");
        }
        parent.getChildren().add(child);
        userRepository.save(parent);
        return new BasicResponse(true, "ההורה " + parent.getUsername() + " קושר לתלמיד " + child.getUsername());
    }

    @Transactional
    public BasicResponse unlinkParentFromChild(String schoolCode, String parentUserId, String childUserId) {
        ParentEntity parent = parentRepository.findByUserId(parentUserId);
        if (parent == null) {
            return new BasicResponse(false, "ההורה לא נמצא");
        }
        boolean removed = parent.getChildren().removeIf(c -> c.getUserId().equals(childUserId)
                && c.getSchoolName() != null && schoolCode.equals(c.getSchoolName().getSchoolCode()));
        if (!removed) {
            return new BasicResponse(false, "ההורה אינו מקושר לתלמיד זה בבית הספר");
        }
        parentRepository.save(parent);
        return new BasicResponse(true, "הקישור הוסר");
    }

    public BasicResponse getChildren(String parentId) {
        if (parentRepository.findByUserId(parentId) == null) {
            return new BasicResponse(false, "ההורה לא נמצא");
        }
        List<ChildSummaryDTO> children = parentRepository.findChildrenOfParent(parentId);
        BasicResponse<List<ChildSummaryDTO>> response = new BasicResponse<>(true,
                children.isEmpty() ? "עדיין לא קושרו ילדים לחשבון. פנו למזכירות בית הספר" : null);
        response.setData(children);
        return response;
    }

    @Transactional
    public BasicResponse getChildOverview(String parentId, String childId) {
        if (!parentRepository.isParentOf(parentId, childId)) {
            return new BasicResponse(false, "אין הרשאה לצפות בנתוני תלמיד זה");
        }
        ChildOverviewDTO overview = new ChildOverviewDTO();
        overview.setChild(parentRepository.findChildrenOfParent(parentId).stream()
                .filter(c -> c.getUserId().equals(childId)).findFirst().orElse(null));

        List<GradeRecordDTO> results = new ArrayList<>(schoolAnalyticsRepository.findGradeRecordsByStudent(childId));
        results.sort(Comparator.comparing((GradeRecordDTO r) -> parseStart(r.getTestStartTime()),
                Comparator.nullsLast(Comparator.reverseOrder())));
        overview.setTestResults(results);
        overview.setTeacherTestsAverage(results.isEmpty() ? null
                : SchoolAnalyticsService.round(results.stream().mapToInt(GradeRecordDTO::getScore).average().orElse(0)));
        List<AnalyticsBucketDTO> bySubject = SchoolAnalyticsService.bucket(results,
                r -> r.getSubject() == null ? SchoolAnalyticsService.NO_VALUE : r.getSubject());
        bySubject.sort(Comparator.comparing(AnalyticsBucketDTO::getLabel));
        overview.setAverageBySubject(bySubject);
        overview.setAssignedTestsCount(schoolAnalyticsRepository.countAssignedTeacherTests(childId));

        overview.setPracticeTestsCount(schoolAnalyticsRepository.countPracticeResults(childId));
        Double practiceAverage = schoolAnalyticsRepository.averagePracticeScore(childId);
        overview.setPracticeTestsAverage(practiceAverage == null ? null : SchoolAnalyticsService.round(practiceAverage));

        StudentEntity child = studentRepository.findStudentByStudentId(childId);
        if (child != null && child.getStudentProgressEntity() != null
                && child.getStudentProgressEntity().getSkillLevelsBySubTopic() != null) {
            overview.setSkillLevels(new TreeMap<>(child.getStudentProgressEntity().getSkillLevelsBySubTopic()));
        }
        for (Object[] row : attendanceRecordRepository.countByStatusForStudent(childId)) {
            overview.getAttendanceCounts().put(String.valueOf(row[0]), (Long) row[1]);
        }
        overview.setAttendanceAvailable(!overview.getAttendanceCounts().isEmpty());

        BasicResponse<ChildOverviewDTO> response = new BasicResponse<>(true, null);
        response.setData(overview);
        return response;
    }

    private static LocalDateTime parseStart(String value) {
        try {
            return value == null ? null : LocalDateTime.parse(value.trim(), TEST_START_FORMAT);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
