package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.SearchResultDTO;
import org.example.learningprojectserver.entities.UserEntity;
import org.example.learningprojectserver.repository.SchoolManagerRepository;
import org.example.learningprojectserver.repository.SearchQueryRepository;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.repository.TeacherRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.response.PageResponse;
import org.example.learningprojectserver.utils.LocalizedMessages;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Global search for the Ctrl+K palette, scoped by role:
 * system admin - schools; school manager - students, teachers, classes and exams of the school;
 * teacher - students and classes of the school and the teacher's own exams; student - own exams.
 * Roles are not enforced by SecurityConfig (all routes permitAll), so the scope comes from the
 * role stored for userId, not from the JWT.
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int PER_TYPE = 5;

    private final UserRepository userRepository;
    private final SchoolManagerRepository schoolManagerRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SearchQueryRepository searchQueryRepository;
    private final LocalizedMessages messages;

    @Transactional(readOnly = true)
    public BasicResponse search(String userId, String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            BasicResponse response = new BasicResponse(true, messages.get("search.tooShort"));
            response.setData(List.of());
            return response;
        }
        UserEntity user = userRepository.findUserByUserId(userId);
        if (user == null) {
            return new BasicResponse(false, messages.get("common.userNotFound"));
        }
        String p = likePattern(q);
        List<SearchResultDTO> results = new ArrayList<>();
        switch (user.getRole()) {
            case SYSTEM_ADMIN -> results.addAll(searchQueryRepository.schools(p, PER_TYPE * 2));
            case SCHOOLMANAGER -> {
                String code = schoolManagerRepository.findSchoolCodeByUserId(userId);
                if (code != null) {
                    results.addAll(searchQueryRepository.studentsInSchool(code, p, PER_TYPE));
                    results.addAll(searchQueryRepository.teachersInSchool(code, p, PER_TYPE));
                    results.addAll(searchQueryRepository.classesInSchool(code, p, PER_TYPE));
                    results.addAll(searchQueryRepository.examsInSchool(code, p, PER_TYPE));
                }
            }
            case TEACHER -> {
                String code = teacherRepository.findSchoolCodeByUserId(userId);
                if (code != null) {
                    results.addAll(searchQueryRepository.studentsInSchool(code, p, PER_TYPE));
                    results.addAll(searchQueryRepository.classesInSchool(code, p, PER_TYPE));
                }
                results.addAll(searchQueryRepository.examsOfTeacher(userId, p, PER_TYPE));
            }
            case STUDENT -> results.addAll(searchQueryRepository.examsOfStudent(userId, p, PER_TYPE));
        }
        BasicResponse response = new BasicResponse(true, null);
        response.setData(results);
        return response;
    }

    /** The school's students one page at a time, optionally filtered by name. */
    @Transactional(readOnly = true)
    public BasicResponse studentsPage(String userId, String query, Integer page, Integer size) {
        UserEntity user = userRepository.findUserByUserId(userId);
        if (user == null) {
            return new BasicResponse(false, messages.get("common.userNotFound"));
        }
        String code = switch (user.getRole()) {
            case SCHOOLMANAGER -> schoolManagerRepository.findSchoolCodeByUserId(userId);
            case TEACHER -> teacherRepository.findSchoolCodeByUserId(userId);
            case STUDENT -> studentRepository.findSchoolCodeByUserId(userId);
            default -> null;
        };
        if (code == null || user.getRole() == org.example.learningprojectserver.enums.Role.STUDENT) {
            return new BasicResponse(false, messages.get("common.badRequest"));
        }
        int pageNumber = PageResponse.clampPage(page);
        int pageSize = PageResponse.clampSize(size, 20);
        String p = likePattern(query == null ? "" : query.trim());
        long total = searchQueryRepository.countStudents(code, p);
        BasicResponse response = new BasicResponse(true, null);
        response.setData(new PageResponse<>(searchQueryRepository.studentsPage(code, p, pageNumber, pageSize),
                pageNumber, pageSize, total, (int) Math.ceil(total / (double) pageSize)));
        return response;
    }

    // Escapes LIKE wildcards in the user's text and wraps it for a "contains" match.
    static String likePattern(String q) {
        String escaped = q.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
