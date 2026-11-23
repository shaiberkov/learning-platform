package org.example.learningprojectserver.service.livequiz;

import org.example.learningprojectserver.dto.*;
import org.example.learningprojectserver.entities.ClassRoomEntity;
import org.example.learningprojectserver.entities.StudentEntity;
import org.example.learningprojectserver.entities.TeacherEntity;
import org.example.learningprojectserver.notification.publisher.NotificationEventPublisher;
import org.example.learningprojectserver.repository.TeacherRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.questionbank.QuestionBankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveQuizServiceTest {

    @Mock TeacherRepository teacherRepository;
    @Mock NotificationEventPublisher publisher;
    @Mock QuestionBankService questionBankService;
    @InjectMocks LiveQuizService service;

    @BeforeEach
    void setUp() {
        ClassRoomEntity classRoom = new ClassRoomEntity();
        classRoom.setId(7L);
        classRoom.setName("ח1");
        classRoom.getStudents().add(student("s1", "דנה"));
        classRoom.getStudents().add(student("s2", "יואב"));
        classRoom.getStudents().add(student("s3", "אלה"));
        TeacherEntity teacher = new TeacherEntity();
        teacher.setUserId("t1");
        teacher.setUsername("המורה");
        teacher.getTeachingClassRooms().add(classRoom);
        lenient().when(teacherRepository.findByUserId("t1")).thenReturn(teacher);
    }

    @Test
    void fullRoundWithBankQuestionRecordsStatistics() {
        String code = service.start("t1", 7L).getData().code();
        assertEquals(6, code.length());

        BankQuestionDTO bank = new BankQuestionDTO();
        bank.setId(42L);
        bank.setQuestionText("2+2?");
        bank.setAnswer("4");
        bank.setOptions(List.of("3", "4", "5"));
        when(questionBankService.findUsableQuestion("t1", 42L)).thenReturn(bank);
        LiveQuestionRequest request = new LiveQuestionRequest();
        request.setBankQuestionId(42L);
        assertTrue(service.pushQuestion("t1", code, request).isSuccess());

        LiveQuizStateDTO studentView = service.getCurrent("s1").getData();
        assertEquals("2+2?", studentView.questionText());
        assertNull(studentView.correctAnswer(), "answer must stay hidden while the question is open");

        assertTrue(service.answer("s1", code, "4").isSuccess());
        assertTrue(service.answer("s2", code, " 3 ").isSuccess());
        assertFalse(service.answer("s2", code, "4").isSuccess(), "a second answer is refused");
        assertFalse(service.answer("outsider", code, "4").isSuccess());

        LiveQuizResultsDTO results = service.getResults("t1", null).getData();
        assertEquals(3, results.totalStudents());
        assertEquals(2, results.answeredCount());
        assertEquals(1, results.correctCount());
        assertEquals(1, results.answerCounts().get("4"));
        assertEquals(1, results.answerCounts().get("3"));
        assertEquals(0, results.answerCounts().get("5"));

        assertTrue(service.closeQuestion("t1", code).isSuccess());
        verify(questionBankService).recordUsage(42L, 2, 1);
        assertFalse(service.answer("s3", code, "4").isSuccess(), "closed question takes no answers");

        LiveQuizStateDTO revealed = service.getCurrent("s1").getData();
        assertEquals("4", revealed.correctAnswer());
        assertEquals(Boolean.TRUE, revealed.myAnswerCorrect());

        LiveQuizResultsDTO last = service.end("t1", code).getData();
        assertFalse(last.active());
        assertEquals("דנה", last.scoreboard().get(0).studentName());
        assertNull(service.getCurrent("s1").getData());
        verify(publisher, atLeastOnce()).publish(anyCollection(), any());
    }

    @Test
    void typedQuestionIsValidatedAndOtherTeachersAreRefused() {
        String code = service.start("t1", 7L).getData().code();
        LiveQuestionRequest bad = new LiveQuestionRequest();
        bad.setQuestionText("שאלה");
        bad.setAnswer("ב");
        bad.setOptions(List.of("א", "ג"));
        assertFalse(service.pushQuestion("t1", code, bad).isSuccess());

        LiveQuestionRequest open = new LiveQuestionRequest();
        open.setQuestionText("בירת צרפת?");
        open.setAnswer("Paris");
        assertFalse(service.pushQuestion("other-teacher", code, open).isSuccess());
        assertTrue(service.pushQuestion("t1", code, open).isSuccess());
        assertTrue(service.answer("s3", code, "  paris ").isSuccess());
        assertEquals(1, service.getResults("t1", code).getData().correctCount());
        service.closeQuestion("t1", code);
        verify(questionBankService, never()).recordUsage(any(), anyInt(), anyInt());
    }

    @Test
    void startRefusesAClassTheTeacherDoesNotTeach() {
        BasicResponse<LiveQuizResultsDTO> response = service.start("t1", 99L);
        assertFalse(response.isSuccess());
    }

    private static StudentEntity student(String id, String name) {
        StudentEntity s = new StudentEntity();
        s.setUserId(id);
        s.setUsername(name);
        return s;
    }
}
