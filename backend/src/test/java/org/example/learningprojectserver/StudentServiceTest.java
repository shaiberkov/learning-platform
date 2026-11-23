package org.example.learningprojectserver;



import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.repository.*;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.response.SubmitAnswerResponse;
import org.example.learningprojectserver.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    // 1. הגדרת כל הרכיבים שאנחנו רוצים "לזייף" כדי לבדוק רק את הלוגיקה של הסרביס
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private StudentProgressRepository studentProgressRepository;
    @Mock
    private StudentQuestionHistoryRepository studentQuestionHistoryRepository;
    @Mock
    private org.example.learningprojectserver.service.MasteryService masteryService;
    @Mock
    private org.example.learningprojectserver.service.ReviewService reviewService;
    @Mock
    private org.example.learningprojectserver.service.EngagementEvents engagementEvents;

    // 2. הזרקת ה"זיופים" לתוך הסרביס שאותו אנחנו בודקים
    @InjectMocks
    private StudentService studentService;

    private StudentEntity mockStudent;
    private StudentProgressEntity mockProgress;
    private StudentQuestionHistoryEntity mockHistory;
    private QuestionEntity mockQuestion;
    private ClassRoomEntity mockClassRoom;

    private final String TEST_USER_ID = "user123";
    private final Long TEST_QUESTION_ID = 1L;
    private final String SUB_TOPIC = "שברים";

    @BeforeEach
    void setUp() {
        // הכנת נתוני בסיס (Arrange) שרצים לפני כל טסט
        mockClassRoom = new ClassRoomEntity();

        mockClassRoom.setId(10L);
        mockProgress = new StudentProgressEntity();

        // אתחול מפות ההתקדמות כדי למנוע NullPointerException
        Map<String, Integer> successStreak = new HashMap<>();
        successStreak.put(SUB_TOPIC, 0);
        mockProgress.setSubTopicSuccessStreak(successStreak);

        Map<String, Integer> skillLevels = new HashMap<>();
        skillLevels.put(SUB_TOPIC, 1);
        mockProgress.setSkillLevelsBySubTopic(skillLevels);

        Map<String, Integer> incorrectStreak = new HashMap<>();
        incorrectStreak.put(SUB_TOPIC, 0);
        mockProgress.setSubTopicIncorrectStreak(incorrectStreak);

        mockHistory = new StudentQuestionHistoryEntity();

        mockStudent = new StudentEntity();
        mockStudent.setClassRoom(mockClassRoom);
        mockStudent.setStudentProgressEntity(mockProgress);
        mockStudent.setQuestionHistoryEntity(mockHistory);

        mockQuestion = new QuestionEntity();
        mockQuestion.setAnswer("42"); // התשובה הנכונה לשאלה שלנו
    }

    @Test
    void submitAnswer_CorrectAnswer_IncreasesSuccessStreak() {
        // Arrange
        when(studentRepository.findStudentWithAllData(TEST_USER_ID)).thenReturn(mockStudent);
        when(questionRepository.findQuestionById(TEST_QUESTION_ID)).thenReturn(mockQuestion);

        // Act
        BasicResponse response = studentService.submitAnswer(TEST_USER_ID, TEST_QUESTION_ID, SUB_TOPIC, "42");

        // Assert
        assertTrue(response.isSuccess());
        SubmitAnswerResponse data = (SubmitAnswerResponse) response.getData();
        assertTrue(data.isCorrect());

        // וידוא שרצף ההצלחות עלה ל-1 ורצף הכישלונות התאפס
        assertEquals(1, mockProgress.getSubTopicSuccessStreak().get(SUB_TOPIC));
        assertEquals(0, mockProgress.getSubTopicIncorrectStreak().get(SUB_TOPIC));

        // וידוא שהרפוזיטורי נשמר
        verify(studentProgressRepository, times(1)).save(mockProgress);
        verify(studentQuestionHistoryRepository, times(1)).save(mockHistory);
    }

    @Test
    void submitAnswer_ThreeCorrectAnswers_IncreasesSkillLevelAndResetsStreak() {
        // Arrange
        // נגדיר שהתלמיד כבר ענה על 2 תשובות נכונות ברצף לפני השאלה הזו
        mockProgress.getSubTopicSuccessStreak().put(SUB_TOPIC, 2);

        when(studentRepository.findStudentWithAllData(TEST_USER_ID)).thenReturn(mockStudent);
        when(questionRepository.findQuestionById(TEST_QUESTION_ID)).thenReturn(mockQuestion);

        // Act
        studentService.submitAnswer(TEST_USER_ID, TEST_QUESTION_ID, SUB_TOPIC, "42 "); // הוספתי רווח בכוונה לבדוק את ה-trim()

        // Assert
        // התלמיד ענה נכון פעם שלישית ברצף - הרמה אמורה לעלות ל-2 והרצף להתאפס
        assertEquals(2, mockProgress.getSkillLevelsBySubTopic().get(SUB_TOPIC));
        assertEquals(0, mockProgress.getSubTopicSuccessStreak().get(SUB_TOPIC));
    }

    @Test
    void submitAnswer_ThreeIncorrectAnswers_DropsSkillLevelAndResetsStreak() {
        // Arrange
        // התלמיד ברמה 2, וכבר טעה פעמיים ברצף
        mockProgress.getSkillLevelsBySubTopic().put(SUB_TOPIC, 2);
        mockProgress.getSubTopicIncorrectStreak().put(SUB_TOPIC, 2);

        when(studentRepository.findStudentWithAllData(TEST_USER_ID)).thenReturn(mockStudent);
        when(questionRepository.findQuestionById(TEST_QUESTION_ID)).thenReturn(mockQuestion);

        // Act - שולחים תשובה שגויה
        BasicResponse response = studentService.submitAnswer(TEST_USER_ID, TEST_QUESTION_ID, SUB_TOPIC, "wrong_answer");

        // Assert
        SubmitAnswerResponse data = (SubmitAnswerResponse) response.getData();
        assertFalse(data.isCorrect());

        // רצף הכישלונות הגיע ל-3: הרמה צריכה לרדת ל-1, והרצף להתאפס
        assertEquals(1, mockProgress.getSkillLevelsBySubTopic().get(SUB_TOPIC));
        assertEquals(0, mockProgress.getSubTopicIncorrectStreak().get(SUB_TOPIC));
        assertEquals(0, mockProgress.getSubTopicSuccessStreak().get(SUB_TOPIC));
    }

    @Test
    void submitAnswer_UserNotFound_ReturnsErrorResponse() {
        // Arrange
        when(studentRepository.findStudentWithAllData("unknown_user")).thenReturn(null);

        // Act
        BasicResponse response = studentService.submitAnswer("unknown_user",
                TEST_QUESTION_ID, SUB_TOPIC, "42");

        // Assert
        assertFalse(response.isSuccess());


        // מוודאים ששום מידע לא נשמר למסד הנתונים
        verify(studentProgressRepository, never()).save(any());
    }
}