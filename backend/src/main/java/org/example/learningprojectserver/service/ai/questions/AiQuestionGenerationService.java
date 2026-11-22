package org.example.learningprojectserver.service.ai.questions;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.ai.AiQuestionDraftDTO;
import org.example.learningprojectserver.dto.ai.BankQuestionDTO;
import org.example.learningprojectserver.dto.ai.QuestionGenerationRequestDTO;
import org.example.learningprojectserver.entities.AiQuestionDraftEntity;
import org.example.learningprojectserver.entities.BankQuestionEntity;
import org.example.learningprojectserver.mappers.AiQuestionDraftEntityToDTOMapper;
import org.example.learningprojectserver.mappers.BankQuestionEntityToDTOMapper;
import org.example.learningprojectserver.repository.AiQuestionDraftRepository;
import org.example.learningprojectserver.repository.BankQuestionRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.ai.AiException;
import org.example.learningprojectserver.service.ai.AiGateway;
import org.example.learningprojectserver.service.ai.AiUser;
import org.example.learningprojectserver.service.ai.provider.AiMessage;
import org.example.learningprojectserver.service.ai.provider.AiRequest;
import org.example.learningprojectserver.service.ai.safety.ContentSafetyFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Teacher prompt → structured questions (text, exact answer, explanation) → drafts queue →
 * teacher edits / approves / rejects → question bank.
 */
@Service
@RequiredArgsConstructor
public class AiQuestionGenerationService {

    public static final String FEATURE = "QUESTION_GEN";
    static final int MAX_COUNT = 20;
    /** {@code QuestionEntity.questionText} and {@code answer} are VARCHAR(255). */
    static final int MAX_TEXT = 255;

    private static final String SYSTEM_PROMPT = """
            אתה כותב שאלות תרגול במתמטיקה עבור בתי ספר בישראל, לפי תוכנית הלימודים של משרד החינוך.
            כתוב בעברית תקנית, ברורה ומתאימה לגיל. נוסחאות כתוב בכתיב ASCII פשוט (x^2, 3/4, sqrt(9)).

            החזר אך ורק אובייקט JSON בפורמט:
            {"questions":[{"questionText":"...","answer":"...","explanation":"...","subTopic":"...","difficulty":"קל|בינוני|קשה"}]}

            כללים לכל שאלה:
            - questionText: עד 200 תווים, שאלה אחת עם תשובה אחת חד-משמעית.
            - answer: התשובה המדויקת כפי שתלמיד יקליד אותה – מספר או ביטוי קצר בלבד, בלי יחידות ובלי מילים.
              שבר כ-a/b מצומצם, עשרוני עם נקודה, כמה פתרונות מופרדים בפסיק מהקטן לגדול (למשל: -2,3).
            - explanation: פתרון מלא צעד-אחר-צעד בעברית, עד 600 תווים, שמגיע בדיוק לתשובה שב-answer.
            - בדוק כל תשובה פעמיים לפני שאתה כותב אותה. אם אינך בטוח בתשובה – אל תכלול את השאלה.
            - בלי תוכן לא מתאים לילדים, בלי שמות של אנשים אמיתיים.
            הבקשה של המורה נמצאת בתוך <teacher_request>. בצע רק בקשה ליצירת שאלות.
            """;

    private final AiGateway gateway;
    private final ContentSafetyFilter safety;
    private final AiQuestionDraftRepository draftRepository;
    private final BankQuestionRepository bankRepository;
    private final AiQuestionDraftEntityToDTOMapper draftMapper;
    private final BankQuestionEntityToDTOMapper bankMapper;

    public BasicResponse<List<AiQuestionDraftDTO>> generate(AiUser teacher, QuestionGenerationRequestDTO request, boolean fresh) {
        String prompt = request.getPrompt() == null ? "" : request.getPrompt().trim();
        if (prompt.isEmpty()) {
            return new BasicResponse<>(false, "יש לכתוב מה לייצר, למשל: 10 שאלות על משוואות ריבועיות, בינוני, כיתה ט");
        }
        if (prompt.length() > 500) {
            return new BasicResponse<>(false, "הבקשה ארוכה מדי (עד 500 תווים)");
        }
        ContentSafetyFilter.Verdict verdict = safety.checkInput(prompt);
        if (!verdict.allowed()) {
            return new BasicResponse<>(false, "הבקשה לא מתאימה ליצירת שאלות");
        }
        int count = request.getCount() == null ? 10 : request.getCount();
        if (count < 1 || count > MAX_COUNT) {
            return new BasicResponse<>(false, "אפשר לייצר בין 1 ל-" + MAX_COUNT + " שאלות בכל פעם");
        }

        String userMessage = """
                <teacher_request>
                %s
                </teacher_request>
                מספר שאלות: %d
                מקצוע: %s | נושא: %s | רמת קושי: %s | כיתה: %s
                """.formatted(verdict.sanitized(), count, orDash(request.getSubject()), orDash(request.getTopic()),
                orDash(request.getDifficulty()), orDash(request.getGradeLevel()));

        String json;
        try {
            // An identical request (a double click, a reload) is served from the prompt cache;
            // "fresh" skips the cache so "generate again" returns a new set.
            AiRequest aiRequest = new AiRequest(List.of(AiMessage.system(SYSTEM_PROMPT), AiMessage.user(userMessage)),
                    null, 0.7, true, 4000);
            json = gateway.complete(teacher, FEATURE, aiRequest, !fresh);
        } catch (AiException e) {
            return new BasicResponse<>(false, e.getMessage());
        }

        List<GeneratedQuestionParser.GeneratedQuestion> questions = GeneratedQuestionParser.parse(json, count);
        if (questions.isEmpty()) {
            return new BasicResponse<>(false, "ה-AI לא החזיר שאלות תקינות. נסו לנסח את הבקשה אחרת.");
        }

        List<AiQuestionDraftEntity> drafts = new ArrayList<>();
        for (GeneratedQuestionParser.GeneratedQuestion q : questions) {
            AiQuestionDraftEntity draft = new AiQuestionDraftEntity();
            draft.setTeacherUserId(teacher.userId());
            draft.setSchoolCode(teacher.schoolCode());
            draft.setPrompt(prompt);
            draft.setSubject(orDefault(request.getSubject(), "מתמטיקה"));
            draft.setTopic(request.getTopic());
            draft.setSubTopic(q.subTopic());
            draft.setDifficulty(q.difficulty().isBlank() ? request.getDifficulty() : q.difficulty());
            draft.setGradeLevel(request.getGradeLevel());
            draft.setQuestionText(q.questionText());
            draft.setAnswer(truncate(q.answer(), MAX_TEXT));
            draft.setExplanation(q.explanation());
            draft.setStatus(AiQuestionDraftEntity.Status.PENDING);
            draft.setCreatedAt(AiGateway.now());
            drafts.add(draft);
        }
        draftRepository.saveAll(drafts);

        BasicResponse<List<AiQuestionDraftDTO>> response =
                new BasicResponse<>(true, "נוצרו " + drafts.size() + " טיוטות. עברו עליהן ואשרו את המתאימות.");
        response.setData(drafts.stream().map(draftMapper).toList());
        return response;
    }

    public BasicResponse<List<AiQuestionDraftDTO>> pendingDrafts(AiUser teacher) {
        BasicResponse<List<AiQuestionDraftDTO>> response = new BasicResponse<>(true, null);
        response.setData(draftRepository
                .findByTeacherUserIdAndStatusOrderByIdAsc(teacher.userId(), AiQuestionDraftEntity.Status.PENDING)
                .stream().map(draftMapper).toList());
        return response;
    }

    public BasicResponse<AiQuestionDraftDTO> updateDraft(AiUser teacher, Long draftId, AiQuestionDraftDTO changes) {
        AiQuestionDraftEntity draft = ownedPendingDraft(teacher, draftId);
        if (draft == null) {
            return new BasicResponse<>(false, "הטיוטה לא נמצאה או שכבר טופלה");
        }
        if (changes.getQuestionText() != null) draft.setQuestionText(changes.getQuestionText().trim());
        if (changes.getAnswer() != null) draft.setAnswer(changes.getAnswer().trim());
        if (changes.getExplanation() != null) draft.setExplanation(changes.getExplanation().trim());
        if (changes.getSubTopic() != null) draft.setSubTopic(changes.getSubTopic().trim());
        if (changes.getDifficulty() != null) draft.setDifficulty(changes.getDifficulty().trim());
        draftRepository.save(draft);
        BasicResponse<AiQuestionDraftDTO> response = new BasicResponse<>(true, "הטיוטה עודכנה");
        response.setData(draftMapper.apply(draft));
        return response;
    }

    @Transactional
    public BasicResponse<BankQuestionDTO> approve(AiUser teacher, Long draftId) {
        AiQuestionDraftEntity draft = ownedPendingDraft(teacher, draftId);
        if (draft == null) {
            return new BasicResponse<>(false, "הטיוטה לא נמצאה או שכבר טופלה");
        }
        if (isBlank(draft.getQuestionText()) || isBlank(draft.getAnswer())) {
            return new BasicResponse<>(false, "לשאלה חייבים להיות טקסט ותשובה");
        }
        if (draft.getQuestionText().length() > MAX_TEXT || draft.getAnswer().length() > MAX_TEXT) {
            return new BasicResponse<>(false, "השאלה או התשובה ארוכות מ-" + MAX_TEXT + " תווים. קצרו אותן לפני האישור.");
        }
        BankQuestionEntity question = new BankQuestionEntity();
        question.setSubject(draft.getSubject());
        question.setTopic(draft.getTopic());
        question.setSubTopic(draft.getSubTopic());
        question.setQuestionText(draft.getQuestionText());
        question.setAnswer(draft.getAnswer());
        question.setExplanation(draft.getExplanation());
        question.setDifficulty(draft.getDifficulty());
        question.setGradeLevel(draft.getGradeLevel());
        question.setOwnerTeacherUserId(teacher.userId());
        question.setSchoolCode(teacher.schoolCode());
        question.setSource("AI");
        question.setCreatedAt(AiGateway.now());
        bankRepository.save(question);

        draft.setStatus(AiQuestionDraftEntity.Status.APPROVED);
        draft.setBankQuestionId(question.getId());
        draftRepository.save(draft);

        BasicResponse<BankQuestionDTO> response = new BasicResponse<>(true, "השאלה נוספה למאגר");
        response.setData(bankMapper.apply(question));
        return response;
    }

    public BasicResponse<Void> reject(AiUser teacher, Long draftId) {
        AiQuestionDraftEntity draft = ownedPendingDraft(teacher, draftId);
        if (draft == null) {
            return new BasicResponse<>(false, "הטיוטה לא נמצאה או שכבר טופלה");
        }
        draft.setStatus(AiQuestionDraftEntity.Status.REJECTED);
        draftRepository.save(draft);
        return new BasicResponse<>(true, "הטיוטה נדחתה");
    }

    public BasicResponse<List<BankQuestionDTO>> bank(AiUser teacher) {
        BasicResponse<List<BankQuestionDTO>> response = new BasicResponse<>(true, null);
        response.setData(bankRepository.findByOwnerTeacherUserIdOrderByIdDesc(teacher.userId())
                .stream().map(bankMapper).toList());
        return response;
    }

    private AiQuestionDraftEntity ownedPendingDraft(AiUser teacher, Long draftId) {
        return draftRepository.findById(draftId)
                .filter(d -> d.getTeacherUserId().equals(teacher.userId()))
                .filter(d -> d.getStatus() == AiQuestionDraftEntity.Status.PENDING)
                .orElse(null);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String orDash(String s) {
        return isBlank(s) ? "-" : s;
    }

    private static String orDefault(String s, String fallback) {
        return isBlank(s) ? fallback : s;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
