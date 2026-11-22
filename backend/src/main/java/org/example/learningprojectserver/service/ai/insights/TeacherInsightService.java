package org.example.learningprojectserver.service.ai.insights;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.ai.InsightStatsDTO;
import org.example.learningprojectserver.dto.ai.TeacherInsightDTO;
import org.example.learningprojectserver.entities.TeacherInsightEntity;
import org.example.learningprojectserver.entities.TestAnswerRecordEntity;
import org.example.learningprojectserver.entities.UserEntity;
import org.example.learningprojectserver.repository.TeacherInsightRepository;
import org.example.learningprojectserver.repository.TestAnswerRecordRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.ai.AiException;
import org.example.learningprojectserver.service.ai.AiGateway;
import org.example.learningprojectserver.service.ai.AiUser;
import org.example.learningprojectserver.service.ai.provider.AiMessage;
import org.example.learningprojectserver.service.ai.provider.AiRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Weekly teacher insights: statistics computed here ({@link InsightCalculator}) plus a short
 * AI summary written from pseudonymised numbers (no names or ids leave the server).
 * Runs weekly from {@code jobs/AiInsightsJob} and on demand from the teacher screen.
 */
@Service
@RequiredArgsConstructor
public class TeacherInsightService {

    public static final String FEATURE = "INSIGHTS";
    private static final Logger log = LoggerFactory.getLogger(TeacherInsightService.class);

    private static final String SYSTEM_PROMPT = """
            אתה יועץ פדגוגי שכותב למורה למתמטיקה סיכום שבועי קצר על הכיתה שלו, על סמך נתונים מספריים בלבד.
            כתוב בעברית, עד 1200 תווים, בשלושה חלקים קצרים עם כותרות: "מה בולט השבוע", "תלמידים שכדאי לשים לב אליהם", "שאלות לבדיקה".
            - תלמידים מופיעים בכינויים (S1, S2...). השתמש רק בכינויים, ואל תנחש מי הם.
            - "שאלה חשודה" היא שאלה שרוב התלמידים בחרו בה אותה תשובה שגויה: המלץ לבדוק את מפתח התשובה או טעות מושגית משותפת, והצע איך לתרגל אותה.
            - תן 2–3 המלצות מעשיות לשבוע הבא. אל תמציא נתונים שלא מופיעים.
            """;

    private final TestAnswerRecordRepository recordRepository;
    private final TeacherInsightRepository insightRepository;
    private final UserRepository userRepository;
    private final AiGateway gateway;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BasicResponse<TeacherInsightDTO> latest(AiUser teacher) {
        TeacherInsightEntity entity = insightRepository.findFirstByTeacherUserIdOrderByCreatedAtDesc(teacher.userId());
        BasicResponse<TeacherInsightDTO> response = new BasicResponse<>(true,
                entity == null ? "עדיין אין סיכום. הוא נוצר אוטומטית בכל יום ראשון, או בלחיצה על \"צור עכשיו\"." : null);
        response.setData(entity == null ? null : toDto(entity));
        return response;
    }

    /** Builds and stores the insight for the 7 days before now. */
    public BasicResponse<TeacherInsightDTO> generate(AiUser teacher) {
        LocalDateTime end = AiGateway.now();
        LocalDateTime start = end.minusDays(7);
        List<TestAnswerRecordEntity> records = recordRepository.findByTeacherUserIdAndCreatedAtBetween(teacher.userId(), start, end);
        InsightStatsDTO stats = InsightCalculator.compute(records, studentNames(records));

        String summary;
        boolean aiGenerated = false;
        if (records.isEmpty()) {
            summary = "לא נבדקו מבחנים של התלמידים שלך בשבוע האחרון, אז אין עדיין על מה לסכם.";
        } else {
            try {
                summary = gateway.complete(teacher, FEATURE, new AiRequest(
                        List.of(AiMessage.system(SYSTEM_PROMPT), AiMessage.user(anonymizedFacts(stats))),
                        null, 0.3, false, 900), true);
                aiGenerated = true;
            } catch (AiException e) {
                summary = "הסיכום המילולי לא נכתב: " + e.getMessage() + " הנתונים למטה מעודכנים.";
            }
        }

        TeacherInsightEntity entity = new TeacherInsightEntity();
        entity.setTeacherUserId(teacher.userId());
        entity.setSchoolCode(teacher.schoolCode());
        entity.setPeriodStart(start);
        entity.setPeriodEnd(end);
        entity.setStatsJson(writeStats(stats));
        entity.setSummary(summary);
        entity.setAiGenerated(aiGenerated);
        entity.setCreatedAt(end);
        insightRepository.save(entity);

        BasicResponse<TeacherInsightDTO> response = new BasicResponse<>(true, null);
        response.setData(toDto(entity));
        return response;
    }

    /** The facts the AI sees: topics, pseudonyms and question texts, never names or ids. */
    static String anonymizedFacts(InsightStatsDTO stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("סה\"כ תשובות: ").append(stats.getTotalAnswers())
                .append(", תלמידים: ").append(stats.getStudentCount()).append("\n\nדיוק לפי נושא:\n");
        stats.getTopics().forEach(t -> sb.append("- ").append(t.getTopic()).append(": ")
                .append(t.getAccuracyPct()).append("% נכון מתוך ").append(t.getAnswered()).append("\n"));
        sb.append("\nתלמידים מתקשים (פחות מ-50% בנושא):\n");
        if (stats.getStrugglingStudents().isEmpty()) {
            sb.append("- אין\n");
        }
        stats.getStrugglingStudents().forEach(s -> sb.append("- ").append(s.getLabel()).append(" ב").append(s.getTopic())
                .append(": ").append(s.getAccuracyPct()).append("% מתוך ").append(s.getAnswered()).append("\n"));
        sb.append("\nשאלות חשודות:\n");
        if (stats.getSuspiciousQuestions().isEmpty()) {
            sb.append("- אין\n");
        }
        stats.getSuspiciousQuestions().forEach(q -> sb.append("- \"").append(q.getQuestionText())
                .append("\" (נושא: ").append(q.getTopic()).append(", תשובה במפתח: ").append(q.getCorrectAnswer())
                .append("): ").append(q.getCommonWrongPct()).append("% ענו \"").append(q.getCommonWrongAnswer())
                .append("\"\n"));
        return sb.toString();
    }

    private Map<String, String> studentNames(List<TestAnswerRecordEntity> records) {
        List<String> ids = records.stream().map(TestAnswerRecordEntity::getStudentUserId).distinct().toList();
        Map<String, String> names = new HashMap<>();
        if (!ids.isEmpty()) {
            for (UserEntity user : userRepository.findUsersByUserIds(ids)) {
                names.put(user.getUserId(), user.getUsername());
            }
        }
        return names;
    }

    private String writeStats(InsightStatsDTO stats) {
        try {
            return objectMapper.writeValueAsString(stats);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private TeacherInsightDTO toDto(TeacherInsightEntity entity) {
        InsightStatsDTO stats;
        try {
            stats = objectMapper.readValue(entity.getStatsJson(), InsightStatsDTO.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("Unreadable insight stats {}: {}", entity.getId(), e.getMessage());
            stats = new InsightStatsDTO();
        }
        return new TeacherInsightDTO(entity.getId(), entity.getPeriodStart(), entity.getPeriodEnd(), entity.getCreatedAt(),
                entity.getSummary(), entity.isAiGenerated(), stats);
    }
}
