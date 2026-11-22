package org.example.learningprojectserver.security;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.repository.AccessQueryRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ownership checks used from @PreAuthorize as {@code @authz.<method>(...)}.
 * The URL rules in SecurityConfig decide which roles reach a controller; these methods decide
 * whether the caller may touch the specific user, school or test named in the request.
 * A system admin passes every ownership check.
 */
@Component("authz")
@RequiredArgsConstructor
public class AccessGuard {

    private final AccessQueryRepository accessQueryRepository;

    public boolean isAdmin() {
        return Role.SYSTEM_ADMIN.name().equals(CurrentUser.role());
    }

    /** The request names the caller themself. */
    public boolean isSelf(String userId) {
        if (isAdmin()) {
            return true;
        }
        String me = CurrentUser.userId();
        return me != null && me.equals(userId);
    }

    public boolean isSelfUsername(String username) {
        if (isAdmin()) {
            return true;
        }
        String me = CurrentUser.username();
        return me != null && me.equals(username);
    }

    /** The request names the caller and the caller's own role (upcoming events). */
    public boolean isSelfWithRole(String userId, Role role) {
        if (isAdmin()) {
            return true;
        }
        return isSelf(userId) && role != null && role.name().equals(CurrentUser.role());
    }

    /** The school code is the caller's own school. */
    public boolean canAccessSchool(String schoolCode) {
        if (isAdmin()) {
            return true;
        }
        String mine = schoolCodeOf(CurrentUser.userId(), CurrentUser.role());
        return mine != null && mine.equals(schoolCode);
    }

    /** A school manager may manage the teachers and students of their own school only. */
    public boolean canManageUser(String targetUserId) {
        if (isAdmin()) {
            return true;
        }
        if (!Role.SCHOOLMANAGER.name().equals(CurrentUser.role()) || targetUserId == null) {
            return false;
        }
        String mine = accessQueryRepository.findManagerSchoolCode(CurrentUser.userId());
        if (mine == null) {
            return false;
        }
        String teacherSchool = accessQueryRepository.findTeacherSchoolCode(targetUserId);
        String studentSchool = accessQueryRepository.findStudentSchoolCode(targetUserId);
        return mine.equals(teacherSchool) || mine.equals(studentSchool);
    }

    /** The teacher is the caller and every listed student sits in one of the teacher's classes. */
    public boolean teachesStudents(String teacherId, List<String> studentIds) {
        if (isAdmin()) {
            return true;
        }
        if (!isSelf(teacherId)) {
            return false;
        }
        if (studentIds == null || studentIds.isEmpty()) {
            return true; // the service rejects an empty list with its own message
        }
        Set<String> distinct = new HashSet<>(studentIds);
        return accessQueryRepository.countStudentsInTeacherClasses(teacherId, distinct) == distinct.size();
    }

    /** The student is the caller and the teacher test was given to them. */
    public boolean isAssignedToTeacherTest(String userId, Long testId) {
        if (isAdmin()) {
            return true;
        }
        return isSelf(userId) && testId != null
                && accessQueryRepository.countTeacherTestAssignments(testId, userId) > 0;
    }

    /** The student is the caller and the practice test is theirs. */
    public boolean ownsPracticeTest(String userId, Long testId) {
        if (isAdmin()) {
            return true;
        }
        return isSelf(userId) && testId != null
                && accessQueryRepository.countPracticeTestsOwnedBy(testId, userId) > 0;
    }

    /** A student sees a test given to them or their own practice test; a teacher sees the tests they wrote. */
    public boolean canViewTest(Long testId) {
        if (isAdmin()) {
            return true;
        }
        String me = CurrentUser.userId();
        if (me == null || testId == null) {
            return false;
        }
        String role = CurrentUser.role();
        if (Role.STUDENT.name().equals(role)) {
            return accessQueryRepository.countTeacherTestAssignments(testId, me) > 0
                    || accessQueryRepository.countPracticeTestsOwnedBy(testId, me) > 0;
        }
        if (Role.TEACHER.name().equals(role)) {
            return accessQueryRepository.countTeacherTestsCreatedBy(testId, me) > 0;
        }
        return false;
    }

    /** The school of a user in the given role, or null (system admins have no school). */
    public String schoolCodeOf(String userId, String role) {
        if (userId == null || role == null) {
            return null;
        }
        return switch (role) {
            case "STUDENT" -> accessQueryRepository.findStudentSchoolCode(userId);
            case "TEACHER" -> accessQueryRepository.findTeacherSchoolCode(userId);
            case "SCHOOLMANAGER" -> accessQueryRepository.findManagerSchoolCode(userId);
            default -> null;
        };
    }

    public String currentSchoolCode() {
        return schoolCodeOf(CurrentUser.userId(), CurrentUser.role());
    }

}
