package org.example.learningprojectserver.service.community;

import org.example.learningprojectserver.dto.BankQuestionDTO;
import org.example.learningprojectserver.dto.community.ClassRefDTO;
import org.example.learningprojectserver.dto.community.PeerQuestionDTOs.AnswerResultDTO;
import org.example.learningprojectserver.dto.community.PeerQuestionDTOs.PeerQuestionDTO;
import org.example.learningprojectserver.dto.community.PeerQuestionDTOs.PracticeQuestionDTO;
import org.example.learningprojectserver.dto.community.PeerQuestionDTOs.SubmitRequest;
import org.example.learningprojectserver.entities.BankQuestionEntity;
import org.example.learningprojectserver.entities.PeerQuestionEntity;
import org.example.learningprojectserver.enums.PeerQuestionStatus;
import org.example.learningprojectserver.mappers.PeerQuestionEntityToDTOMapper;
import org.example.learningprojectserver.repository.BankQuestionRepository;
import org.example.learningprojectserver.repository.PeerQuestionRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.questionbank.QuestionBankService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeerQuestionServiceTest {

    @Mock PeerQuestionRepository peerQuestionRepository;
    @Mock BankQuestionRepository bankQuestionRepository;
    @Mock QuestionBankService questionBankService;
    @Mock ClassCommunitySupport support;
    @Spy PeerQuestionEntityToDTOMapper mapper = new PeerQuestionEntityToDTOMapper();
    @InjectMocks PeerQuestionService service;

    private static SubmitRequest request(String correct, String... distractors) {
        SubmitRequest r = new SubmitRequest();
        r.setQuestionText("כמה זה 3 כפול 4?");
        r.setCorrectAnswer(correct);
        r.setDistractors(List.of(distractors));
        r.setTopic("כפל");
        r.setExplanation("3+3+3+3");
        return r;
    }

    private static PeerQuestionEntity pending(long id) {
        PeerQuestionEntity q = new PeerQuestionEntity();
        q.setId(id);
        q.setClassRoomId(7L);
        q.setAuthorUserId("s1");
        q.setAuthorName("דנה");
        q.setQuestionText("כמה זה 3 כפול 4?");
        q.setCorrectAnswer("12");
        q.setDistractors("7\n34");
        q.setTopic("כפל");
        q.setExplanation("3+3+3+3");
        q.setStatus(PeerQuestionStatus.PENDING);
        q.setCreatedAt(LocalDateTime.now());
        return q;
    }

    @Test
    void validateNeedsTwoDifferentDistractors() {
        assertNull(PeerQuestionService.validate(request("12", "7", "34")));
        assertNotNull(PeerQuestionService.validate(request("12", "7")));
        assertNotNull(PeerQuestionService.validate(request("12", "7", " 7 ")), "duplicates count once");
        assertNotNull(PeerQuestionService.validate(request("12", "7", "12")), "a distractor equal to the answer");
        assertNotNull(PeerQuestionService.validate(request("12", "1", "2", "3", "4", "5", "6")));
        assertNotNull(PeerQuestionService.validate(request(" ", "1", "2")));
    }

    @Test
    void studentSubmitsPendingQuestionAndTeachersAreNotified() {
        when(support.resolveClass("s1", "STUDENT", null)).thenReturn(new ClassRefDTO(7L, "ז1", "S1"));
        when(support.nameOf("s1")).thenReturn("דנה");
        when(support.teachersOf(7L)).thenReturn(List.of("t1"));

        BasicResponse<PeerQuestionDTO> response = service.submit("s1", "STUDENT", request("12", "7", "34", ""));

        assertTrue(response.isSuccess());
        ArgumentCaptor<PeerQuestionEntity> saved = ArgumentCaptor.forClass(PeerQuestionEntity.class);
        verify(peerQuestionRepository).save(saved.capture());
        assertEquals(PeerQuestionStatus.PENDING, saved.getValue().getStatus());
        assertEquals("7\n34", saved.getValue().getDistractors());
        assertEquals(7L, saved.getValue().getClassRoomId());
        verify(support).notify(eq(List.of("t1")), anyString(), anyString(), eq("דנה"));
    }

    @Test
    void submitIsRefusedWhenTooManyArePending() {
        when(support.resolveClass("s1", "STUDENT", null)).thenReturn(new ClassRefDTO(7L, "ז1", "S1"));
        when(peerQuestionRepository.countByAuthorUserIdAndStatus("s1", PeerQuestionStatus.PENDING))
                .thenReturn((long) PeerQuestionService.MAX_PENDING_PER_STUDENT);

        BasicResponse<PeerQuestionDTO> response = service.submit("s1", "STUDENT", request("12", "7", "34"));

        assertFalse(response.isSuccess());
        verify(peerQuestionRepository, never()).save(any());
    }

    @Test
    void teacherApprovalAddsToBankWithStudentSourceAndCreditsAuthor() {
        PeerQuestionEntity q = pending(3);
        when(peerQuestionRepository.findById(3L)).thenReturn(Optional.of(q));
        when(support.belongsTo("t1", "TEACHER", 7L)).thenReturn(true);
        when(peerQuestionRepository.switchStatus(3L, PeerQuestionStatus.PENDING, PeerQuestionStatus.APPROVED)).thenReturn(1);
        BankQuestionDTO added = new BankQuestionDTO();
        added.setId(99L);
        BasicResponse<BankQuestionDTO> addedResponse = new BasicResponse<>(true, "ok");
        addedResponse.setData(added);
        when(questionBankService.addQuestion(eq("t1"), any())).thenReturn(addedResponse);
        BankQuestionEntity bank = new BankQuestionEntity();
        bank.setId(99L);
        when(bankQuestionRepository.findById(99L)).thenReturn(Optional.of(bank));

        BasicResponse<PeerQuestionDTO> response = service.approve("t1", "TEACHER", 3L, "שאלה מעולה");

        assertTrue(response.isSuccess());
        ArgumentCaptor<BankQuestionDTO> bankDto = ArgumentCaptor.forClass(BankQuestionDTO.class);
        verify(questionBankService).addQuestion(eq("t1"), bankDto.capture());
        assertEquals("12", bankDto.getValue().getAnswer());
        assertEquals(List.of("12", "7", "34"), bankDto.getValue().getOptions());
        assertEquals("STUDENT", bank.getSource());
        assertEquals("3+3+3+3", bank.getExplanation());
        assertEquals(PeerQuestionStatus.APPROVED, q.getStatus());
        assertEquals(99L, q.getBankQuestionId());
        verify(support).notify(eq(List.of("s1")), contains("אושרה"), contains("שאלה מעולה"), any());
    }

    @Test
    void approvingTwiceIsRefused() {
        PeerQuestionEntity q = pending(3);
        q.setStatus(PeerQuestionStatus.APPROVED);
        when(peerQuestionRepository.findById(3L)).thenReturn(Optional.of(q));
        when(support.belongsTo("t1", "TEACHER", 7L)).thenReturn(true);

        assertFalse(service.approve("t1", "TEACHER", 3L, null).isSuccess());
        verifyNoInteractions(questionBankService);
    }

    @Test
    void approvalThatLosesTheRaceAddsNothingToTheBank() {
        when(peerQuestionRepository.findById(3L)).thenReturn(Optional.of(pending(3)));
        when(support.belongsTo("t1", "TEACHER", 7L)).thenReturn(true);
        when(peerQuestionRepository.switchStatus(3L, PeerQuestionStatus.PENDING, PeerQuestionStatus.APPROVED)).thenReturn(0);

        assertFalse(service.approve("t1", "TEACHER", 3L, null).isSuccess());
        verifyNoInteractions(questionBankService);
    }

    @Test
    void teacherOfAnotherClassCannotReview() {
        when(peerQuestionRepository.findById(3L)).thenReturn(Optional.of(pending(3)));
        when(support.belongsTo("t2", "TEACHER", 7L)).thenReturn(false);

        assertFalse(service.approve("t2", "TEACHER", 3L, null).isSuccess());
        assertFalse(service.reject("t2", "TEACHER", 3L, "לתקן").isSuccess());
        verifyNoInteractions(questionBankService);
    }

    @Test
    void rejectNeedsANoteForTheStudent() {
        assertFalse(service.reject("t1", "TEACHER", 3L, " ").isSuccess());
        PeerQuestionEntity q = pending(3);
        when(peerQuestionRepository.findById(3L)).thenReturn(Optional.of(q));
        when(support.belongsTo("t1", "TEACHER", 7L)).thenReturn(true);

        when(peerQuestionRepository.switchStatus(3L, PeerQuestionStatus.PENDING, PeerQuestionStatus.REJECTED)).thenReturn(1);
        assertTrue(service.reject("t1", "TEACHER", 3L, "המסיח 7 קל מדי").isSuccess());
        assertEquals(PeerQuestionStatus.REJECTED, q.getStatus());
        verify(support).notify(eq(List.of("s1")), anyString(), contains("המסיח"), any());
    }

    @Test
    void practiceHidesTheAnswerAndKeepsAllOptions() {
        PeerQuestionEntity q = pending(3);
        q.setStatus(PeerQuestionStatus.APPROVED);
        when(support.resolveClass("s2", "STUDENT", null)).thenReturn(new ClassRefDTO(7L, "ז1", "S1"));
        when(peerQuestionRepository.findByClassRoomIdAndStatusOrderByReviewedAtDesc(7L, PeerQuestionStatus.APPROVED))
                .thenReturn(List.of(q));

        List<PracticeQuestionDTO> list = service.practice("s2", "STUDENT", null).getData();

        assertEquals(1, list.size());
        assertEquals("דנה", list.get(0).getAuthorName());
        assertFalse(list.get(0).isMine());
        assertEquals(3, list.get(0).getOptions().size());
        assertTrue(list.get(0).getOptions().containsAll(List.of("12", "7", "34")));
    }

    @Test
    void classmateAnswerIsCheckedAndCountedOnTheBankQuestion() {
        PeerQuestionEntity q = pending(3);
        q.setStatus(PeerQuestionStatus.APPROVED);
        q.setBankQuestionId(99L);
        when(peerQuestionRepository.findById(3L)).thenReturn(Optional.of(q));
        when(support.belongsTo("s2", "STUDENT", 7L)).thenReturn(true);

        BasicResponse<AnswerResultDTO> wrong = service.answer("s2", "STUDENT", 3L, "7");
        BasicResponse<AnswerResultDTO> right = service.answer("s2", "STUDENT", 3L, " 12 ");

        assertFalse(wrong.getData().isCorrect());
        assertEquals("12", wrong.getData().getCorrectAnswer());
        assertTrue(right.getData().isCorrect());
        verify(peerQuestionRepository).recordBankAnswer(99L, 0);
        verify(peerQuestionRepository).recordBankAnswer(99L, 1);
    }

    @Test
    void authorAnsweringOwnQuestionIsNotCounted() {
        PeerQuestionEntity q = pending(3);
        q.setStatus(PeerQuestionStatus.APPROVED);
        q.setBankQuestionId(99L);
        when(peerQuestionRepository.findById(3L)).thenReturn(Optional.of(q));
        when(support.belongsTo("s1", "STUDENT", 7L)).thenReturn(true);

        assertTrue(service.answer("s1", "STUDENT", 3L, "12").getData().isCorrect());
        verify(peerQuestionRepository, never()).recordBankAnswer(anyLong(), anyInt());
    }

    @Test
    void pendingQuestionIsNotInPractice() {
        when(peerQuestionRepository.findById(3L)).thenReturn(Optional.of(pending(3)));
        assertFalse(service.answer("s2", "STUDENT", 3L, "12").isSuccess());
    }
}
