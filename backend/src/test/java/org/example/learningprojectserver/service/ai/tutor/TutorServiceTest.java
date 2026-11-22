package org.example.learningprojectserver.service.ai.tutor;

import org.example.learningprojectserver.dto.ai.TutorRequestDTO;
import org.example.learningprojectserver.entities.AiConversationMessageEntity;
import org.example.learningprojectserver.entities.PracticeQuestionEntity;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.mappers.AiConversationMessageEntityToDTOMapper;
import org.example.learningprojectserver.repository.AiConversationMessageRepository;
import org.example.learningprojectserver.repository.QuestionRepository;
import org.example.learningprojectserver.service.ai.AiGateway;
import org.example.learningprojectserver.service.ai.AiProperties;
import org.example.learningprojectserver.service.ai.AiUser;
import org.example.learningprojectserver.service.ai.provider.AiRequest;
import org.example.learningprojectserver.service.ai.safety.ContentSafetyFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TutorServiceTest {

    @Mock
    private AiGateway gateway;
    @Mock
    private ExamGuard examGuard;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AiConversationMessageRepository conversationRepository;

    private TutorService service;
    private RecordingSink sink;
    private final AiUser student = new AiUser("s1", Role.STUDENT, "school-1");

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        ReflectionTestUtils.setField(properties, "tutorHistoryMessages", 10);
        service = new TutorService(gateway, properties, new ContentSafetyFilter(), examGuard, questionRepository,
                conversationRepository, new AiConversationMessageEntityToDTOMapper());
        sink = new RecordingSink();
        when(conversationRepository.findRecent(anyString(), any(), any())).thenReturn(List.of());

        PracticeQuestionEntity question = new PracticeQuestionEntity();
        question.setId(5L);
        question.setQuestionText("12 + 7 = ?");
        question.setAnswer("19");
        when(questionRepository.findQuestionById(5L)).thenReturn(question);
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    private static TutorRequestDTO request(String message, String studentAnswer) {
        TutorRequestDTO dto = new TutorRequestDTO();
        dto.setQuestionId(5L);
        dto.setMessage(message);
        dto.setStudentAnswer(studentAnswer);
        return dto;
    }

    @SuppressWarnings("unchecked")
    private void modelAnswers(String... deltas) {
        when(gateway.stream(any(), anyString(), any(AiRequest.class), any())).thenAnswer(inv -> {
            Consumer<String> onDelta = inv.getArgument(3);
            StringBuilder full = new StringBuilder();
            for (String d : deltas) {
                onDelta.accept(d);
                full.append(d);
            }
            return full.toString();
        });
    }

    @Test
    void duringAnExamTheTutorRefusesWithoutCallingTheModel() {
        when(examGuard.isInActiveExam("s1")).thenReturn(true);

        service.reply(student, request("עזרה", ""), sink);

        assertEquals(List.of(ExamGuard.EXAM_ACTIVE_MESSAGE), sink.errors);
        verify(gateway, never()).stream(any(), anyString(), any(), any());
    }

    @Test
    void examQuestionsAreRefusedAtAnyTime() {
        when(examGuard.isExamQuestion(5L)).thenReturn(true);

        service.reply(student, request("עזרה", ""), sink);

        assertEquals(List.of(ExamGuard.EXAM_QUESTION_MESSAGE), sink.errors);
        verify(gateway, never()).stream(any(), anyString(), any(), any());
    }

    @Test
    void distressIsAnsweredLocallyAndFlaggedForStaff() {
        service.reply(student, request("אני רוצה למות", ""), sink);

        assertEquals(ContentSafetyFilter.DISTRESS_REPLY, sink.replaced);
        verify(gateway, never()).stream(any(), anyString(), any(), any());
        ArgumentCaptor<AiConversationMessageEntity> saved = ArgumentCaptor.forClass(AiConversationMessageEntity.class);
        verify(conversationRepository).save(saved.capture());
        assertTrue(saved.getValue().isFlagged());
        assertEquals("DISTRESS", saved.getValue().getFlagReason());
    }

    @Test
    void streamsTheAnswerAndSendsTheQuestionContext() {
        modelAnswers("מה ", "קורה כשמחברים ", "עשרות?");

        service.reply(student, request("לא הבנתי", "18"), sink);

        assertEquals(List.of("מה ", "קורה כשמחברים ", "עשרות?"), sink.deltas);
        assertNull(sink.replaced);
        assertTrue(sink.done);
        ArgumentCaptor<AiRequest> sent = ArgumentCaptor.forClass(AiRequest.class);
        verify(gateway).stream(eq(student), eq(TutorService.FEATURE), sent.capture(), any());
        String system = sent.getValue().messages().get(0).content();
        assertTrue(system.contains("12 + 7 = ?") && system.contains("18") && system.contains("(לא נכונה)"));
        assertTrue(sent.getValue().messages().get(1).content().startsWith("<student_input>"));
    }

    @Test
    void anAnswerThatGivesTheResultAwayIsWithheld() {
        modelAnswers("התשובה ", "היא ", "19", " כמובן");

        service.reply(student, request("מה התשובה?", ""), sink);

        assertEquals(List.of("התשובה ", "היא "), sink.deltas);
        assertEquals(TutorPrompts.ANSWER_WITHHELD, sink.replaced);
    }

    @Test
    void onceTheStudentSolvedItTheTutorMayConfirmTheAnswer() {
        modelAnswers("נכון! ", "19 ", "זו התשובה");

        service.reply(student, request("צדקתי?", "19"), sink);

        assertNull(sink.replaced);
        assertEquals(3, sink.deltas.size());
    }

    private static final class RecordingSink implements TutorService.TutorSink {
        final List<String> deltas = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
        String replaced;
        boolean done;

        @Override
        public void delta(String text) {
            deltas.add(text);
        }

        @Override
        public void replace(String text) {
            replaced = text;
        }

        @Override
        public void done(int remainingToday) {
            done = true;
        }

        @Override
        public void error(String hebrewMessage) {
            errors.add(hebrewMessage);
        }
    }
}
