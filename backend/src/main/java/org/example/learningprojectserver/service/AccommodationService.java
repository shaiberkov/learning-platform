package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.AccommodationDTO;
import org.example.learningprojectserver.dto.StudentBriefDTO;
import org.example.learningprojectserver.entities.StudentAccommodationEntity;
import org.example.learningprojectserver.repository.StudentAccommodationRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Per-student accommodations. Extra time is set by a teacher of the student's class and applied
 * by the exam screen to every timer; read-aloud and the dyslexia font are the student's choice.
 */
@Service
@RequiredArgsConstructor
public class AccommodationService {

    public static final int MAX_EXTRA_TIME_PERCENT = 100;

    private final StudentAccommodationRepository repository;

    /** Minutes the student gets for an exam of baseMinutes, rounded up. */
    public static int adjustedMinutes(int baseMinutes, int extraTimePercent) {
        if (baseMinutes <= 0 || extraTimePercent <= 0) return baseMinutes;
        return (int) Math.ceil(baseMinutes * (100 + extraTimePercent) / 100.0);
    }

    private static AccommodationDTO toDTO(String userId, String name, String className, StudentAccommodationEntity a) {
        return a == null
                ? new AccommodationDTO(userId, name, className, 0, false, false)
                : new AccommodationDTO(userId, name, className, a.getExtraTimePercent(), a.isReadAloud(), a.isDyslexiaFont());
    }

    private BasicResponse ok(String message, Object data) {
        BasicResponse<Object> response = new BasicResponse<>(true, message);
        response.setData(data);
        return response;
    }

    @Transactional(readOnly = true)
    public BasicResponse get(String studentId) {
        StudentAccommodationEntity a = repository.findByStudentUserId(studentId).orElse(null);
        return ok(null, toDTO(studentId, null, null, a));
    }

    @Transactional(readOnly = true)
    public BasicResponse getTeacherStudents(String teacherId) {
        List<StudentBriefDTO> students = repository.findStudentsOfTeacher(teacherId);
        Map<String, StudentAccommodationEntity> byId = students.isEmpty() ? Map.of()
                : repository.findByStudentUserIdIn(students.stream().map(StudentBriefDTO::userId).toList()).stream()
                .collect(Collectors.toMap(StudentAccommodationEntity::getStudentUserId, Function.identity(), (x, y) -> x));
        List<AccommodationDTO> list = students.stream()
                .map(s -> toDTO(s.userId(), s.username(), s.className(), byId.get(s.userId())))
                .toList();
        return ok(null, list);
    }

    @Transactional
    public BasicResponse setExtraTime(String teacherId, String studentId, int percent) {
        if (percent < 0 || percent > MAX_EXTRA_TIME_PERCENT) {
            return new BasicResponse(false, "תוספת הזמן חייבת להיות בין 0 ל-" + MAX_EXTRA_TIME_PERCENT + " אחוז");
        }
        boolean teachesStudent = repository.findStudentsOfTeacher(teacherId).stream()
                .anyMatch(s -> s.userId().equals(studentId));
        if (!teachesStudent) {
            return new BasicResponse(false, "ניתן לעדכן התאמות רק לתלמידים בכיתות שלך");
        }
        StudentAccommodationEntity a = repository.findByStudentUserId(studentId)
                .orElseGet(() -> new StudentAccommodationEntity(studentId));
        a.setExtraTimePercent(percent);
        repository.save(a);
        return ok("תוספת הזמן עודכנה", toDTO(studentId, null, null, a));
    }

    @Transactional
    public BasicResponse setPreferences(String studentId, boolean readAloud, boolean dyslexiaFont) {
        StudentAccommodationEntity a = repository.findByStudentUserId(studentId)
                .orElseGet(() -> new StudentAccommodationEntity(studentId));
        a.setReadAloud(readAloud);
        a.setDyslexiaFont(dyslexiaFont);
        repository.save(a);
        return ok("ההעדפות נשמרו", toDTO(studentId, null, null, a));
    }
}
