package org.example.learningprojectserver;

import org.example.learningprojectserver.dto.MessageDTO;
import org.example.learningprojectserver.dto.SearchResultDTO;
import org.example.learningprojectserver.dto.StudentDTO;
import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.notification.NotificationType;
import org.example.learningprojectserver.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the JPQL added for messages, notifications and search against an in-memory H2 database,
 * so a query that does not parse or map fails here instead of at server start-up.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:lp;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@Import(SearchQueryRepository.class)
class CommunicationQueriesTest {

    @Autowired TestEntityManager em;
    @Autowired MessageRepository messageRepository;
    @Autowired MessageAttachmentRepository attachmentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired CalendarTokenRepository calendarTokenRepository;
    @Autowired SearchQueryRepository searchQueryRepository;

    private TeacherEntity teacher;
    private StudentEntity student;
    private StudentEntity student2;

    private static <T extends UserEntity> T user(T u, String id, String name, Role role) {
        u.setUserId(id);
        u.setUsername(name);
        u.setPasswordHash("h");
        u.setSalt("s");
        u.setPhoneNumber("050");
        u.setEmail(id + "@x.test");
        u.setRole(role);
        return u;
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

        teacher = user(new TeacherEntity(), "T1", "רינה כהן", Role.TEACHER);
        teacher.setTeachingSchool(school);
        em.persist(teacher);

        student = user(new StudentEntity(), "S1", "דנה לוי", Role.STUDENT);
        student.setSchoolName(school);
        student.setClassRoom(c);
        em.persist(student);

        student2 = user(new StudentEntity(), "S2", "יוסי אבו", Role.STUDENT);
        student2.setSchoolName(school);
        em.persist(student2);

        TeacherTestEntity test = new TeacherTestEntity();
        test.setSubject("מתמטיקה");
        test.setTopic("שברים");
        test.setStartTime("10/01/2027 09:00");
        test.setTimeLimitMinutes(45);
        test.setTeacher(teacher);
        test.getStudents().add(student);
        em.persist(test);
        student.getTeacherTests().add(test);
        em.flush();
    }

    private MessageEntity message(UserEntity from, UserEntity to, String batch, Boolean pending, Long attachmentId) {
        MessageEntity m = new MessageEntity();
        m.setSender(from);
        m.setReceiver(to);
        m.setTitle("כותרת");
        m.setContent("תוכן");
        m.setSentAt(LocalDateTime.now());
        m.setBatchId(batch);
        m.setThreadId("thread-1");
        m.setPending(pending);
        m.setAttachmentId(attachmentId);
        return em.persist(m);
    }

    @Test
    void messageQueries() {
        MessageAttachmentEntity a = new MessageAttachmentEntity();
        a.setFileName("hw.pdf");
        a.setContentType("application/pdf");
        a.setSizeBytes(3);
        a.setData(new byte[]{1, 2, 3});
        a.setUploaderUserId("T1");
        a.setCreatedAt(LocalDateTime.now());
        a = attachmentRepository.save(a);

        MessageEntity m1 = message(teacher, student, "b1", null, a.getId());
        message(teacher, student2, "b1", null, a.getId());
        message(teacher, student, "b2", true, null); // scheduled, hidden from the student
        m1.setReadAt(LocalDateTime.now());
        em.flush();

        List<MessageDTO> inbox = messageRepository.findMessagesByRecipientUserId("S1");
        assertEquals(1, inbox.size());
        assertEquals("hw.pdf", inbox.get(0).getAttachmentName());
        assertNotNull(inbox.get(0).getReadAt());

        Page<MessageDTO> page = messageRepository.findMessagesPageByRecipientUserId("S1", PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());

        List<Object[]> sent = messageRepository.findSentBatchesBySender("T1", PageRequest.of(0, 10));
        assertEquals(2, sent.size());
        Object[] b1 = sent.stream().filter(r -> "b1".equals(r[0])).findFirst().orElseThrow();
        assertEquals(2L, ((Number) b1[7]).longValue());
        assertEquals(1L, ((Number) b1[8]).longValue());
        assertEquals(0, ((Number) b1[6]).intValue());

        assertEquals(2, messageRepository.findReadReceipts("T1", "b1").size());
        assertEquals(1, messageRepository.findThreadForUser("S1", "thread-1").size());
        assertEquals(3, messageRepository.findThreadMessageIdsSentBy("T1", "thread-1").size());
        assertEquals(1, messageRepository.countAttachmentAccess("S2", a.getId()));
        assertEquals(0, messageRepository.countAttachmentAccess("nobody", a.getId()));
        assertEquals(0, messageRepository.findDueScheduledMessages(LocalDateTime.now()).size());
    }

    @Test
    void notificationQueries() {
        for (int i = 0; i < 3; i++) {
            NotificationEntity n = new NotificationEntity();
            n.setUserId("S1");
            n.setType(NotificationType.SYSTEM_MESSAGE);
            n.setTitle("n" + i);
            n.setCreatedAt(LocalDateTime.now());
            em.persist(n);
        }
        em.flush();
        Long firstId = notificationRepository.findByUserIdOrderByIdDesc("S1", PageRequest.of(0, 10))
                .getContent().get(2).getId();
        assertEquals(2, notificationRepository.findByUserIdAndIdGreaterThanOrderByIdAsc("S1", firstId, PageRequest.of(0, 10)).size());
        assertEquals(3, notificationRepository.countByUserIdAndReadAtIsNull("S1"));
        assertEquals(3, notificationRepository.markAllRead("S1", LocalDateTime.now()));
    }

    @Test
    void searchQueries() {
        List<SearchResultDTO> students = searchQueryRepository.studentsInSchool("S100", "%דנה%", 5);
        assertEquals(1, students.size());
        assertEquals("ז1", students.get(0).getSubtitle());
        assertEquals(1, searchQueryRepository.teachersInSchool("S100", "%רינה%", 5).size());
        assertEquals(1, searchQueryRepository.classesInSchool("S100", "%ז%", 5).size());
        assertEquals(1, searchQueryRepository.examsOfTeacher("T1", "%שבר%", 5).size());
        assertEquals(1, searchQueryRepository.examsInSchool("S100", "%מתמ%", 5).size());
        assertEquals(1, searchQueryRepository.examsOfStudent("S1", "%מתמ%", 5).size());
        assertEquals(1, searchQueryRepository.schools("%אור%", 5).size());

        List<StudentDTO> page = searchQueryRepository.studentsPage("S100", "%%", 0, 10);
        assertEquals(2, page.size()); // includes the student without a class
        assertEquals(2, searchQueryRepository.countStudents("S100", "%%"));
    }

    @Test
    void calendarTokenLookup() {
        CalendarTokenEntity t = new CalendarTokenEntity();
        t.setUserId("S1");
        t.setToken("abc");
        t.setCreatedAt(LocalDateTime.now());
        calendarTokenRepository.save(t);
        assertEquals("S1", calendarTokenRepository.findByToken("abc").getUserId());
    }
}
