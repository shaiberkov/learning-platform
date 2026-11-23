package org.example.learningprojectserver.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.learningprojectserver.dto.SearchResultDTO;
import org.example.learningprojectserver.dto.StudentDTO;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Read-only JPQL queries behind the global search and the paginated student directory.
 * `pattern` is already lower-cased and wrapped in % by the caller. Results are capped by
 * `limit`; username and class name have indexes (idx_user_username, idx_class_room_name).
 */
@Repository
public class SearchQueryRepository {

    @PersistenceContext
    private EntityManager em;

    public List<SearchResultDTO> studentsInSchool(String schoolCode, String pattern, int limit) {
        return em.createQuery("""
                SELECT new org.example.learningprojectserver.dto.SearchResultDTO(
                    'STUDENT', s.userId, s.username, c.name)
                FROM StudentEntity s LEFT JOIN s.classRoom c
                WHERE s.schoolName.schoolCode = :code AND LOWER(s.username) LIKE :p
                ORDER BY s.username""", SearchResultDTO.class)
                .setParameter("code", schoolCode).setParameter("p", pattern)
                .setMaxResults(limit).getResultList();
    }

    public List<SearchResultDTO> teachersInSchool(String schoolCode, String pattern, int limit) {
        return em.createQuery("""
                SELECT new org.example.learningprojectserver.dto.SearchResultDTO(
                    'TEACHER', t.userId, t.username, t.email)
                FROM TeacherEntity t
                WHERE t.teachingSchool.schoolCode = :code AND LOWER(t.username) LIKE :p
                ORDER BY t.username""", SearchResultDTO.class)
                .setParameter("code", schoolCode).setParameter("p", pattern)
                .setMaxResults(limit).getResultList();
    }

    public List<SearchResultDTO> classesInSchool(String schoolCode, String pattern, int limit) {
        return em.createQuery("""
                SELECT new org.example.learningprojectserver.dto.SearchResultDTO(
                    'CLASS', c.name, c.name, g.gradeName)
                FROM ClassRoomEntity c LEFT JOIN c.grade g
                WHERE c.school.schoolCode = :code AND LOWER(c.name) LIKE :p
                ORDER BY c.name""", SearchResultDTO.class)
                .setParameter("code", schoolCode).setParameter("p", pattern)
                .setMaxResults(limit).getResultList();
    }

    public List<SearchResultDTO> examsOfTeacher(String teacherUserId, String pattern, int limit) {
        return em.createQuery("""
                SELECT new org.example.learningprojectserver.dto.SearchResultDTO(
                    'EXAM', CONCAT('', t.id), CONCAT(t.subject, ' - ', t.topic), t.startTime)
                FROM TeacherTestEntity t
                WHERE t.teacher.userId = :uid
                AND (LOWER(t.subject) LIKE :p OR LOWER(t.topic) LIKE :p)
                ORDER BY t.id DESC""", SearchResultDTO.class)
                .setParameter("uid", teacherUserId).setParameter("p", pattern)
                .setMaxResults(limit).getResultList();
    }

    public List<SearchResultDTO> examsInSchool(String schoolCode, String pattern, int limit) {
        return em.createQuery("""
                SELECT new org.example.learningprojectserver.dto.SearchResultDTO(
                    'EXAM', CONCAT('', t.id), CONCAT(t.subject, ' - ', t.topic), t.startTime)
                FROM TeacherTestEntity t
                WHERE t.teacher.teachingSchool.schoolCode = :code
                AND (LOWER(t.subject) LIKE :p OR LOWER(t.topic) LIKE :p)
                ORDER BY t.id DESC""", SearchResultDTO.class)
                .setParameter("code", schoolCode).setParameter("p", pattern)
                .setMaxResults(limit).getResultList();
    }

    public List<SearchResultDTO> examsOfStudent(String studentUserId, String pattern, int limit) {
        return em.createQuery("""
                SELECT new org.example.learningprojectserver.dto.SearchResultDTO(
                    'EXAM', CONCAT('', t.id), CONCAT(t.subject, ' - ', t.topic), t.startTime)
                FROM StudentEntity s JOIN s.teacherTests t
                WHERE s.userId = :uid
                AND (LOWER(t.subject) LIKE :p OR LOWER(t.topic) LIKE :p)
                ORDER BY t.id DESC""", SearchResultDTO.class)
                .setParameter("uid", studentUserId).setParameter("p", pattern)
                .setMaxResults(limit).getResultList();
    }

    public List<SearchResultDTO> schools(String pattern, int limit) {
        return em.createQuery("""
                SELECT new org.example.learningprojectserver.dto.SearchResultDTO(
                    'SCHOOL', s.schoolCode, s.schoolName, s.schoolCode)
                FROM SchoolEntity s
                WHERE LOWER(s.schoolName) LIKE :p OR LOWER(s.schoolCode) LIKE :p
                ORDER BY s.schoolName""", SearchResultDTO.class)
                .setParameter("p", pattern)
                .setMaxResults(limit).getResultList();
    }

    // Same shape as StudentEntityToStudentDTOMapper, but tolerates a student without a class.
    public List<StudentDTO> studentsPage(String schoolCode, String pattern, int page, int size) {
        return em.createQuery("""
                SELECT new org.example.learningprojectserver.dto.StudentDTO(
                    s.username, s.phoneNumber, c.name, g.gradeName)
                FROM StudentEntity s LEFT JOIN s.classRoom c LEFT JOIN c.grade g
                WHERE s.schoolName.schoolCode = :code AND LOWER(s.username) LIKE :p
                ORDER BY s.username""", StudentDTO.class)
                .setParameter("code", schoolCode).setParameter("p", pattern)
                .setFirstResult(page * size).setMaxResults(size)
                .getResultList();
    }

    public long countStudents(String schoolCode, String pattern) {
        return em.createQuery("""
                SELECT COUNT(s) FROM StudentEntity s
                WHERE s.schoolName.schoolCode = :code AND LOWER(s.username) LIKE :p""", Long.class)
                .setParameter("code", schoolCode).setParameter("p", pattern)
                .getSingleResult();
    }
}
