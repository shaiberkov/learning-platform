package org.example.learningprojectserver.repository;

import org.example.learningprojectserver.dto.NoteTopicDTO;
import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** [s04] Runs the flashcard and study-notebook JPQL against in-memory H2, like CommunicationQueriesTest. */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:lp-s04;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class StudyToolsQueriesTest {

    @Autowired TestEntityManager em;
    @Autowired FlashcardRepository flashcardRepository;
    @Autowired FlashcardReviewRepository reviewRepository;
    @Autowired StudyNoteRepository noteRepository;

    private Long classRoomId;

    private static StudentEntity student(String id) {
        StudentEntity s = new StudentEntity();
        s.setUserId(id);
        s.setUsername(id);
        s.setPasswordHash("h");
        s.setSalt("s");
        s.setPhoneNumber("050");
        s.setEmail(id + "@x.test");
        s.setRole(Role.STUDENT);
        return s;
    }

    private static StudyNoteEntity note(String owner, String topic, String targetType, long targetId) {
        StudyNoteEntity n = new StudyNoteEntity();
        n.setStudentUserId(owner);
        n.setKind("NOTE");
        n.setTargetType(targetType);
        n.setTargetId(targetId);
        n.setTopic(topic);
        n.setBody("x");
        n.setCreatedAt(LocalDateTime.now());
        return n;
    }

    @BeforeEach
    void seed() {
        SchoolEntity school = new SchoolEntity();
        school.setSchoolName("אורט");
        school.setSchoolCode("S100");
        em.persist(school);
        SchoolGradeEntity grade = new SchoolGradeEntity();
        grade.setGradeName("ז");
        grade.setSchool(school);
        em.persist(grade);
        ClassRoomEntity c = new ClassRoomEntity();
        c.setName("ז1");
        c.setGrade(grade);
        c.setSchool(school);
        em.persist(c);
        classRoomId = c.getId();

        StudentEntity inClass = student("S1");
        inClass.setSchoolName(school);
        inClass.setClassRoom(c);
        em.persist(inClass);
        em.persist(student("S2"));

        em.persist(note("S1", "משוואות", "QUESTION", 4));
        em.persist(note("S1", "משוואות", "LESSON", 2));
        em.persist(note("S1", "שברים", "QUESTION", 5));
        em.persist(note("S2", "משוואות", "QUESTION", 4));
        em.flush();
    }

    @Test
    void classOfAStudent() {
        assertEquals(classRoomId, flashcardRepository.findClassRoomIdOfStudent("S1"));
        assertNull(flashcardRepository.findClassRoomIdOfStudent("S2"));
    }

    @Test
    void notesFilterByTopicAndTarget() {
        assertEquals(3, noteRepository.search("S1", null, null, null).size());
        assertEquals(2, noteRepository.search("S1", "משוואות", null, null).size());
        assertEquals(1, noteRepository.search("S1", null, "QUESTION", 4L).size());
        assertEquals(List.of(new NoteTopicDTO("משוואות", 2), new NoteTopicDTO("שברים", 1)), noteRepository.countByTopic("S1"));
    }

    @Test
    void dueCardsAndDeleteByCard() {
        FlashcardReviewEntity due = new FlashcardReviewEntity();
        due.setStudentUserId("S1");
        due.setFlashcardId(1L);
        due.setDueDate(LocalDate.of(2026, 1, 1));
        FlashcardReviewEntity later = new FlashcardReviewEntity();
        later.setStudentUserId("S1");
        later.setFlashcardId(2L);
        later.setDueDate(LocalDate.of(2026, 3, 1));
        reviewRepository.saveAll(List.of(due, later));

        LocalDate today = LocalDate.of(2026, 2, 1);
        assertEquals(List.of(1L), reviewRepository.findDue("S1", today).stream().map(FlashcardReviewEntity::getFlashcardId).toList());
        assertEquals(1, reviewRepository.countDue("S1", today));
        assertEquals(2, reviewRepository.countByStudentUserId("S1"));

        reviewRepository.deleteByFlashcardId(1L);
        assertEquals(1, reviewRepository.countByStudentUserId("S1"));
    }
}
