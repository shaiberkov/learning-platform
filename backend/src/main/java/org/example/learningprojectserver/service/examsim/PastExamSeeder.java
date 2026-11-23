package org.example.learningprojectserver.service.examsim;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.entities.PastExamEntity;
import org.example.learningprojectserver.entities.PastExamQuestionEntity;
import org.example.learningprojectserver.entities.PastExamSectionEntity;
import org.example.learningprojectserver.entities.QuestionEntity;
import org.example.learningprojectserver.repository.PastExamRepository;
import org.example.learningprojectserver.service.MathQuestion.MathQuestionFactory;
import org.example.learningprojectserver.service.adaptive.SkillCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fills an empty past-exam bank with two sample exams built from the question generators
 * (MathQuestionFactory), so the simulation works out of the box. They are original practice exams in
 * the Bagrut format (sections, a choice section, a rubric), not copies of real exams. Runs once: when
 * the bank already has an exam nothing is added. A failure is logged and never stops the server.
 */
@Component
@RequiredArgsConstructor
public class PastExamSeeder {

    private static final Logger log = LoggerFactory.getLogger(PastExamSeeder.class);

    private final PastExamRepository pastExamRepository;

    /** One generated question: sub-topic as MathQuestionFactory accepts it, generator level and points. */
    record Spec(String subTopic, int level, int points) {}

    record SectionSpec(String title, String instructions, Integer questionsToCount, List<Spec> questions) {}

    @EventListener(ApplicationReadyEvent.class)
    public void seedIfEmpty() {
        try {
            if (pastExamRepository.count() > 0) return;
            pastExamRepository.save(build("מבחן מתכונת לדוגמה — אלגברה והנדסה אנליטית", 2025, "מועד חורף (לדוגמה)",
                    "מבחן תרגול במבנה בגרות: שני פרקים, בפרק השני בוחרים 2 מתוך 3 שאלות. השאלות נוצרו על ידי מחולל השאלות של המערכת.",
                    60, List.of(
                            new SectionSpec("פרק א׳ — אלגברה", "ענו על כל השאלות בפרק.", null, List.of(
                                    new Spec("מישוואה עם נעלם אחד", 3, 20),
                                    new Spec("מערכת משוואות עם שני נעלמים", 3, 20),
                                    new Spec("מישוואה עם נעלם אחד", 4, 20))),
                            new SectionSpec("פרק ב׳ — הנדסה אנליטית ווקטורים",
                                    "ענו על 2 מתוך 3 השאלות. אם עניתם על כולן, ייספרו שתי התשובות הטובות ביותר.", 2, List.of(
                                    new Spec("נקודת חיתוך בין שני ישרים", 3, 20),
                                    new Spec("מכפלה סקלרית של וקטורים", 3, 20),
                                    new Spec("נקודת חיתוך בין שני ישרים", 4, 20))))));
            pastExamRepository.save(build("מבחן לדוגמה — חשבון ושברים", 2024, "מועד קיץ (לדוגמה)",
                    "מבחן תרגול קצר בחשבון, בשברים ובבעיות מילוליות. השאלות נוצרו על ידי מחולל השאלות של המערכת.",
                    45, List.of(
                            new SectionSpec("פרק א׳ — מספרים שלמים", "ענו על כל השאלות בפרק.", null, List.of(
                                    new Spec("כפל מספרים שלמים", 3, 15),
                                    new Spec("חזקות", 2, 15),
                                    new Spec("שורש ריבועי", 3, 10))),
                            new SectionSpec("פרק ב׳ — שברים ובעיות מילוליות", "ענו על כל השאלות בפרק.", null, List.of(
                                    new Spec("חיבור שברים", 2, 20),
                                    new Spec("חיבור שברים", 3, 20),
                                    new Spec("שאלה מילולית", 2, 20))))));
            log.info("Seeded the past-exam bank with two sample simulations");
        } catch (RuntimeException e) {
            log.warn("Could not seed the past-exam bank: {}", e.getMessage());
        }
    }

    static PastExamEntity build(String title, int year, String session, String description, int minutes,
                                List<SectionSpec> sections) {
        MathQuestionFactory factory = new MathQuestionFactory();
        PastExamEntity exam = new PastExamEntity();
        exam.setTitle(title);
        exam.setSubject(SkillCatalog.MATH);
        exam.setExamYear(year);
        exam.setSessionLabel(session);
        exam.setDescription(description);
        exam.setDurationMinutes(minutes);
        exam.setCreatedAt(LocalDateTime.now());
        int sectionNo = 0;
        for (SectionSpec spec : sections) {
            PastExamSectionEntity section = new PastExamSectionEntity();
            section.setExam(exam);
            section.setPosition(++sectionNo);
            section.setTitle(spec.title());
            section.setInstructions(spec.instructions());
            section.setQuestionsToCount(spec.questionsToCount());
            int questionNo = 0;
            for (Spec q : spec.questions()) {
                QuestionEntity generated = factory.getQuestionGenerator(null, q.subTopic()).generateQuestion(q.level());
                PastExamQuestionEntity question = new PastExamQuestionEntity();
                question.setSection(section);
                question.setPosition(++questionNo);
                question.setText(generated.getQuestionText());
                question.setAnswer(generated.getAnswer());
                question.setPoints(q.points());
                question.setLevel(q.level());
                question.setTopic(generated.getTopic());
                question.setSubTopic(SkillCatalog.canonicalSubTopic(generated.getSubTopic()));
                question.setRubric(rubric(generated.getAnswer(), q.points()));
                section.getQuestions().add(question);
            }
            exam.getSections().add(section);
        }
        return exam;
    }

    /** The grading guide shown after the exam. */
    static String rubric(String answer, int points) {
        int parts = ExamSimGrader.partCount(answer);
        if (parts <= 1) return points + " נק׳ לתשובה סופית נכונה. תשובה שגויה או חסרה: 0 נק׳.";
        return points + " נק׳ לתשובה מלאה ונכונה. ניקוד חלקי: " + formatPart(points / (double) parts)
                + " נק׳ לכל אחד מ-" + parts + " רכיבי התשובה שנמצא נכון.";
    }

    private static String formatPart(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format("%.1f", value);
    }
}
