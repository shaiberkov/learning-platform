package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.dto.*;
import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.enums.AttendanceStatus;
import org.example.learningprojectserver.enums.GradeCategory;
import org.example.learningprojectserver.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the JPQL of the question bank, gradebook and attendance repositories against an in-memory H2
 * database, so a broken query fails here instead of at application startup.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class TeacherToolsRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired BankQuestionRepository bankQuestionRepository;
    @Autowired GradebookAssignmentRepository assignmentRepository;
    @Autowired GradebookGradeRepository gradeRepository;
    @Autowired GradebookWeightRepository weightRepository;
    @Autowired AttendanceRecordRepository attendanceRepository;

    SchoolEntity school;
    ClassRoomEntity classRoom;
    TeacherEntity teacher;
    StudentEntity student;
    LessonEntity lesson;

    @BeforeEach
    void setUp() {
        school = new SchoolEntity();
        school.setSchoolName("בית ספר");
        school.setSchoolCode("S1");
        em.persist(school);

        classRoom = new ClassRoomEntity();
        classRoom.setName("ח1");
        classRoom.setSchool(school);
        em.persist(classRoom);

        teacher = user(new TeacherEntity(), "t1", Role.TEACHER);
        teacher.setTeachingSchool(school);
        teacher.getTeachingClassRooms().add(classRoom);
        em.persist(teacher);

        student = user(new StudentEntity(), "s1", Role.STUDENT);
        student.setClassRoom(classRoom);
        student.setSchoolName(school);
        em.persist(student);
        classRoom.getStudents().add(student);

        ScheduleEntity schedule = new ScheduleEntity();
        schedule.setClassRoom(classRoom);
        em.persist(schedule);

        lesson = new LessonEntity();
        lesson.setTeacher(teacher);
        lesson.setSchedule(schedule);
        lesson.setSubject("מתמטיקה");
        lesson.setDayOfWeek(DayOfWeek.SUNDAY);
        lesson.setStartTime(LocalTime.of(8, 0));
        lesson.setEndTime(LocalTime.of(8, 45));
        em.persist(lesson);
        em.flush();
    }

    @Test
    void bankSearchHonoursOwnershipSharingAndTags() {
        bank("t1", "S1", false, "כמה זה 2+2", "חיבור");
        bank("t2", "S1", true, "כמה זה 3*3", "כפל");
        bank("t2", "S1", false, "פרטי של מורה אחר", "כפל");
        bank("t3", "OTHER", true, "בית ספר אחר", "כפל");

        assertEquals(1, bankQuestionRepository.search("t1", "S1", false, null, null, null, null, null).size());
        assertEquals(2, bankQuestionRepository.search("t1", "S1", true, null, null, null, null, null).size());
        assertEquals(1, bankQuestionRepository.search("t1", "S1", true, null, "כפל", null, null, null).size());
        assertEquals(1, bankQuestionRepository.search("t1", "S1", true, null, null, null, null, "2+2").size());

        Long id = bankQuestionRepository.search("t1", "S1", false, null, null, null, null, null).get(0).getId();
        assertEquals(1, bankQuestionRepository.recordUsage(id, 5, 3));
        em.clear();
        BankQuestionEntity updated = bankQuestionRepository.findById(id).orElseThrow();
        assertEquals(1, updated.getTimesAsked());
        assertEquals(5, updated.getAttempts());
        assertEquals(3, updated.getCorrectAttempts());
    }

    @Test
    void gradebookQueriesReadTeacherTestsAndGrades() {
        TeacherTestEntity test = new TeacherTestEntity();
        test.setTeacher(teacher);
        test.setSubject("מתמטיקה");
        test.setTopic("שברים");
        test.setStartTime("01/01/2026 10:00");
        test.getStudents().add(student);
        em.persist(test);
        TeacherTestResultEntity result = new TeacherTestResultEntity();
        result.setTest(test);
        result.setStudent(student);
        result.setTeacher(teacher);
        result.setScore(85);
        em.persist(result);

        GradebookAssignmentEntity assignment = new GradebookAssignmentEntity();
        assignment.setTeacherId("t1");
        assignment.setClassRoomId(classRoom.getId());
        assignment.setTitle("שיעורי בית 1");
        assignment.setCategory(GradeCategory.HOMEWORK);
        assignment.setAssignedDate(LocalDate.now());
        em.persist(assignment);
        GradebookGradeEntity grade = new GradebookGradeEntity();
        grade.setAssignment(assignment);
        grade.setStudentUserId("s1");
        grade.setScore(90);
        em.persist(grade);
        GradebookWeightEntity weight = new GradebookWeightEntity();
        weight.setTeacherId("t1");
        weight.setClassRoomId(classRoom.getId());
        weight.setCategory(GradeCategory.EXAM);
        weight.setWeight(70);
        em.persist(weight);
        em.flush();

        List<TeacherTestColumnDTO> tests = assignmentRepository.findTeacherTestsForClass("t1", classRoom.getId());
        assertEquals(1, tests.size());
        assertEquals("שברים", tests.get(0).topic());
        List<TeacherTestScoreDTO> scores = assignmentRepository.findTeacherTestScores(List.of(test.getId()));
        assertEquals(85, scores.get(0).score());
        assertEquals("s1", scores.get(0).studentUserId());

        assertEquals(1, assignmentRepository.findByTeacherIdAndClassRoomIdOrderByAssignedDateAscIdAsc("t1", classRoom.getId()).size());
        assertEquals(1, gradeRepository.findByAssignmentIds(List.of(assignment.getId())).size());
        assertTrue(gradeRepository.findByAssignmentAndStudentUserId(assignment, "s1").isPresent());
        assertEquals(1, weightRepository.findByTeacherIdAndClassRoomId("t1", classRoom.getId()).size());

        gradeRepository.deleteByAssignmentId(assignment.getId());
        assertTrue(gradeRepository.findByAssignmentIds(List.of(assignment.getId())).isEmpty());
    }

    @Test
    void attendanceQueriesFindLessonsCountAndReport() {
        LocalDate sunday = LocalDate.of(2026, 11, 22);
        List<AttendanceLessonDTO> lessons = attendanceRepository.findTeacherLessonsOnDay("t1", DayOfWeek.SUNDAY);
        assertEquals(1, lessons.size());
        assertEquals("08:00", lessons.get(0).startTime());
        assertEquals(1, lessons.get(0).studentCount());
        assertTrue(attendanceRepository.findTeacherLessonsOnDay("t1", DayOfWeek.MONDAY).isEmpty());

        AttendanceRecordEntity record = new AttendanceRecordEntity();
        record.setLessonId(lesson.getId());
        record.setLessonDate(sunday);
        record.setStudentUserId("s1");
        record.setStudentName("s1-name");
        record.setStatus(AttendanceStatus.ABSENT);
        record.setClassRoomName("ח1");
        record.setSchoolCode("S1");
        record.setMarkedByTeacherId("t1");
        record.setMarkedAt(LocalDateTime.now());
        em.persist(record);
        em.flush();

        assertEquals(1, attendanceRepository.findByLessonAndDate(lesson.getId(), sunday).size());
        List<Object[]> marked = attendanceRepository.countMarkedByLesson(List.of(lesson.getId()), sunday);
        assertEquals(1L, marked.get(0)[1]);
        assertEquals(1, attendanceRepository.countByStudentAndStatus("s1", AttendanceStatus.ABSENT, sunday.minusDays(29), sunday));

        List<AttendanceCountDTO> counts = attendanceRepository.countForReport("S1", null, null, sunday.minusDays(1), sunday);
        assertEquals(1, counts.size());
        assertEquals(AttendanceStatus.ABSENT, counts.get(0).status());
        assertEquals(1, attendanceRepository.countForReport("S1", "t1", "ח1", sunday, sunday).size());
        assertTrue(attendanceRepository.countForReport("S1", "someone-else", null, sunday, sunday).isEmpty());
        assertEquals(List.of("ח1"), attendanceRepository.findReportClassNames("S1", null));
    }

    private BankQuestionEntity bank(String owner, String schoolCode, boolean shared, String text, String topic) {
        BankQuestionEntity q = new BankQuestionEntity();
        q.setOwnerTeacherId(owner);
        q.setSchoolCode(schoolCode);
        q.setShared(shared);
        q.setQuestionText(text);
        q.setAnswer("x");
        q.setTopic(topic);
        q.setCreatedAt(LocalDateTime.now());
        return em.persist(q);
    }

    private static <T extends UserEntity> T user(T user, String userId, Role role) {
        user.setUserId(userId);
        user.setUsername(userId + "-name");
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPhoneNumber("0500000000");
        user.setEmail(userId + "@example.com");
        user.setRole(role);
        return user;
    }
}
