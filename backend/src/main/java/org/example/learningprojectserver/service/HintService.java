package org.example.learningprojectserver.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.AdaptiveDTOs.*;
import org.example.learningprojectserver.entities.PracticeQuestionEntity;
import org.example.learningprojectserver.entities.QuestionEntity;
import org.example.learningprojectserver.entities.SubTopicHintEntity;
import org.example.learningprojectserver.repository.QuestionRepository;
import org.example.learningprojectserver.repository.SubTopicHintRepository;
import org.example.learningprojectserver.repository.TeacherRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.adaptive.PracticeScoring;
import org.example.learningprojectserver.service.adaptive.SkillCatalog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Hint 1, hint 2 and a step-by-step solution for practice questions. Teachers edit them per
 * sub-topic; sub-topics nobody edited fall back to the built-in defaults below. Each step the
 * student opens is stored on the practice question and lowers the points it can earn.
 */
@Service
@RequiredArgsConstructor
public class HintService {

    private static final int MAX_HINT_LENGTH = 1000;
    private static final int MAX_STEPS_LENGTH = 4000;
    private static final DateTimeFormatter UPDATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private record Template(String hint1, String hint2, List<String> steps) {}

    private static final Map<String, Template> DEFAULTS = new HashMap<>();

    static {
        DEFAULTS.put("חיבור מספרים שלמים", new Template(
                "חבר את המספרים שניים-שניים, משמאל לימין.",
                "פרק כל מספר לעשרות ויחידות: חבר קודם את העשרות ואחר כך את היחידות.",
                List.of("רשום את המספרים אחד מתחת לשני לפי ספרות.", "חבר את עמודת היחידות והעבר עשרת אם צריך.",
                        "חבר את עמודת העשרות (ואת מה שהועבר), וכך הלאה.")));
        DEFAULTS.put("חיסור מספרים שלמים", new Template(
                "שים לב לסדר: מחסרים את המספר השני מהראשון.",
                "אם הספרה העליונה קטנה מהתחתונה, פרוט עשרת מהעמודה שמשמאל.",
                List.of("רשום את המספרים אחד מתחת לשני לפי ספרות.", "חסר בעמודת היחידות, ופרוט עשרת אם צריך.",
                        "המשך לעמודות הבאות.")));
        DEFAULTS.put("כפל מספרים שלמים", new Template(
                "כפל הוא חיבור חוזר: 4×3 זה 4+4+4.",
                "פרק מספר גדול: 12×7 = 10×7 + 2×7.",
                List.of("פרק את אחד המספרים לעשרות ויחידות.", "כפול כל חלק במספר השני.", "חבר את התוצאות.")));
        DEFAULTS.put("חילוק מספרים שלמים", new Template(
                "שאל את עצמך: כמה פעמים המחלק נכנס במחולק?",
                "בדוק בעזרת כפל: התוצאה כפול המחלק צריכה להחזיר את המחולק.",
                List.of("חפש מספר שכפול המחלק נותן את המחולק.", "אם המספר גדול, חלק אותו לחלקים קלים יותר.",
                        "בדוק את התשובה בכפל.")));
        DEFAULTS.put("חזקות", new Template(
                "חזקה היא כפל חוזר של הבסיס בעצמו.",
                "המעריך אומר כמה פעמים כופלים את הבסיס.",
                List.of("זהה את הבסיס ואת המעריך.", "כפול את הבסיס בעצמו לפי מספר הפעמים שבמעריך.")));
        DEFAULTS.put("שורש ריבועי", new Template(
                "חפש מספר שכפול עצמו נותן את המספר שמתחת לשורש.",
                "היעזר בריבועים שאתה מכיר: 1, 4, 9, 16, 25, 36...",
                List.of("רשום ריבועים של מספרים עד שתגיע למספר שבשורש.", "המספר שהריבוע שלו שווה למספר שבשורש הוא התשובה.")));
        DEFAULTS.put("חיבור שברים", new Template(
                "כדי לחבר שברים צריך מכנה משותף.",
                "מצא את הכפולה המשותפת הקטנה של המכנים והרחב את שני השברים אליה.",
                List.of("מצא מכנה משותף.", "הרחב כל שבר למכנה המשותף.", "חבר את המונים והשאר את המכנה.",
                        "צמצם את התוצאה אם אפשר.")));
        DEFAULTS.put("חיסור שברים", new Template(
                "כדי לחסר שברים צריך מכנה משותף.",
                "אחרי ההרחבה חסר רק את המונים.",
                List.of("מצא מכנה משותף.", "הרחב כל שבר למכנה המשותף.", "חסר את המונים והשאר את המכנה.",
                        "צמצם את התוצאה אם אפשר.")));
        DEFAULTS.put("כפל שברים", new Template(
                "בכפל שברים לא צריך מכנה משותף.",
                "כופלים מונה במונה ומכנה במכנה.",
                List.of("כפול את המונים.", "כפול את המכנים.", "צמצם את התוצאה אם אפשר.")));
        DEFAULTS.put("מישוואה עם נעלם אחד", new Template(
                "המטרה: להשאיר את x לבד בצד אחד של המשוואה.",
                "מה שעושים לצד אחד עושים גם לצד השני.",
                List.of("העבר את המספרים הקבועים לצד אחד ואת x לצד השני.", "כנס איברים דומים.",
                        "חלק את שני הצדדים במקדם של x.")));
        DEFAULTS.put("משוואה ריבועית", new Template(
                "סדר את המשוואה בצורה ax² + bx + c = 0.",
                "השתמש בנוסחת השורשים: x = (-b ± √(b² - 4ac)) / 2a.",
                List.of("זהה את a, b ו-c.", "חשב את הדיסקרימיננטה b² - 4ac.", "הצב בנוסחת השורשים וחשב את שני הפתרונות.")));
    }

    private final QuestionRepository questionRepository;
    private final SubTopicHintRepository subTopicHintRepository;
    private final TeacherRepository teacherRepository;

    private static Template genericTemplate(String subTopic) {
        return new Template(
                "קרא שוב את השאלה וזהה מה בדיוק מבקשים ממך ב\"" + subTopic + "\".",
                "פרק את התרגיל לשלבים קטנים ופתור כל שלב בנפרד.",
                List.of("רשום את הנתונים מהשאלה.", "בחר את הכלל או הנוסחה שמתאימים ל\"" + subTopic + "\".",
                        "בצע את החישוב שלב אחר שלב ובדוק את התוצאה."));
    }

    private Template templateFor(String subject, String subTopic) {
        return subTopicHintRepository.findBySubjectAndSubTopic(subject, subTopic)
                .map(h -> new Template(h.getHint1(), h.getHint2(), splitSteps(h.getSolutionSteps())))
                .orElseGet(() -> DEFAULTS.getOrDefault(subTopic, genericTemplate(subTopic)));
    }

    private static List<String> splitSteps(String steps) {
        if (steps == null) return List.of();
        return Arrays.stream(steps.split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /**
     * Opens guidance step 1 (hint 1), 2 (hint 2) or 3 (full solution with the answer) for a
     * practice question. Steps open in order; opening one records it on the question.
     */
    @Transactional
    public BasicResponse getHintStep(String userId, Long questionId, int step) {
        if (step < 1 || step > PracticeScoring.FULL_SOLUTION_STEP) {
            return new BasicResponse(false, "שלב רמז לא תקין");
        }
        QuestionEntity question = questionRepository.findQuestionById(questionId);
        if (!(question instanceof PracticeQuestionEntity practice)) {
            return new BasicResponse(false, "השאלה לא נמצאה");
        }
        if (practice.getProgressEntity() == null || practice.getProgressEntity().getStudent() == null
                || !userId.equals(practice.getProgressEntity().getStudent().getUserId())) {
            return new BasicResponse(false, "השאלה לא שייכת לתלמיד הזה");
        }
        int used = practice.getHintsUsed() == null ? 0 : practice.getHintsUsed();
        if (step > used + 1) {
            return new BasicResponse(false, "יש לפתוח את הרמזים לפי הסדר");
        }
        // After the question was answered, opening guidance no longer changes its score.
        if (practice.getPointsEarned() == null && step > used) {
            practice.setHintsUsed(step);
            questionRepository.save(practice);
            used = step;
        }

        String subTopic = SkillCatalog.canonicalSubTopic(practice.getSubTopic());
        Template template = templateFor(practice.getSubject(), subTopic);
        HintStepDTO dto = switch (step) {
            case 1 -> new HintStepDTO(1, template.hint1(), null, null, used, PracticeScoring.availablePoints(used));
            case 2 -> new HintStepDTO(2, template.hint2(), null, null, used, PracticeScoring.availablePoints(used));
            default -> new HintStepDTO(3, null, template.steps(), practice.getAnswer(), used,
                    PracticeScoring.availablePoints(used));
        };
        BasicResponse response = new BasicResponse(true, null);
        response.setData(dto);
        return response;
    }

    /** Catalog of sub-topics with whether a teacher already edited their hints. */
    public BasicResponse getTemplateCatalog() {
        Set<String> custom = new HashSet<>();
        subTopicHintRepository.findBySubject(SkillCatalog.MATH).forEach(h -> custom.add(h.getSubTopic()));
        List<HintCatalogTopicDTO> topics = SkillCatalog.mathTopics().entrySet().stream()
                .map(e -> new HintCatalogTopicDTO(e.getKey(), e.getValue().stream()
                        .map(s -> new HintCatalogSubTopicDTO(s, custom.contains(s)))
                        .toList()))
                .toList();
        BasicResponse response = new BasicResponse(true, null);
        response.setData(topics);
        return response;
    }

    public BasicResponse getTemplate(String subject, String subTopic) {
        String canonical = SkillCatalog.canonicalSubTopic(subTopic);
        if (!SkillCatalog.MATH.equals(subject) || !SkillCatalog.contains(canonical)) {
            return new BasicResponse(false, "תת-נושא לא נתמך");
        }
        Optional<SubTopicHintEntity> saved = subTopicHintRepository.findBySubjectAndSubTopic(subject, canonical);
        Template t = templateFor(subject, canonical);
        BasicResponse response = new BasicResponse(true, null);
        response.setData(new HintTemplateDTO(subject, SkillCatalog.topicOf(canonical, null), canonical,
                t.hint1(), t.hint2(), t.steps(), saved.isPresent(),
                saved.map(h -> h.getUpdatedAt() == null ? null : h.getUpdatedAt().format(UPDATED_AT_FORMAT)).orElse(null)));
        return response;
    }

    public BasicResponse saveTemplate(String teacherId, HintTemplateRequest request) {
        if (teacherRepository.findByUserId(teacherId) == null) {
            return new BasicResponse(false, "רק מורה יכול לערוך רמזים");
        }
        if (request == null || request.subTopic() == null) {
            return new BasicResponse(false, "חסר תת-נושא");
        }
        String subject = request.subject() == null ? SkillCatalog.MATH : request.subject();
        String subTopic = SkillCatalog.canonicalSubTopic(request.subTopic());
        if (!SkillCatalog.MATH.equals(subject) || !SkillCatalog.contains(subTopic)) {
            return new BasicResponse(false, "תת-נושא לא נתמך");
        }
        String hint1 = request.hint1() == null ? "" : request.hint1().trim();
        String hint2 = request.hint2() == null ? "" : request.hint2().trim();
        List<String> steps = request.solutionSteps() == null ? List.of() : request.solutionSteps().stream()
                .filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toList();
        String joinedSteps = String.join("\n", steps);
        if (hint1.isEmpty() || hint2.isEmpty() || steps.isEmpty()) {
            return new BasicResponse(false, "יש למלא רמז 1, רמז 2 ולפחות שלב אחד בפתרון");
        }
        if (hint1.length() > MAX_HINT_LENGTH || hint2.length() > MAX_HINT_LENGTH || joinedSteps.length() > MAX_STEPS_LENGTH) {
            return new BasicResponse(false, "הטקסט ארוך מדי");
        }

        SubTopicHintEntity entity = subTopicHintRepository.findBySubjectAndSubTopic(subject, subTopic)
                .orElseGet(SubTopicHintEntity::new);
        entity.setSubject(subject);
        entity.setSubTopic(subTopic);
        entity.setTopic(SkillCatalog.topicOf(subTopic, null));
        entity.setHint1(hint1);
        entity.setHint2(hint2);
        entity.setSolutionSteps(joinedSteps);
        entity.setUpdatedByTeacherId(teacherId);
        entity.setUpdatedAt(LocalDateTime.now());
        subTopicHintRepository.save(entity);
        return new BasicResponse(true, "הרמזים נשמרו");
    }
}
