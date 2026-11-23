package org.example.learningprojectserver.service.gradebook;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.GradebookDTO;
import org.example.learningprojectserver.dto.examsim.GradeTargetDTOs.*;
import org.example.learningprojectserver.entities.ClassRoomEntity;
import org.example.learningprojectserver.entities.StudentEntity;
import org.example.learningprojectserver.entities.TeacherEntity;
import org.example.learningprojectserver.enums.GradeCategory;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * "What do I need?" (מחשבון מה אני צריך): for each gradebook of the student's class (one per teacher),
 * the uniform score the student needs in every remaining column to reach a target final grade, using
 * the teacher's category weights. Remaining columns: the teacher's own columns without a grade for the
 * student, and system tests dated today or later that the student has not taken. Read only, not cached.
 */
@Service
@RequiredArgsConstructor
public class GradeTargetService {

    private static final DateTimeFormatter TEST_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final StudentRepository studentRepository;
    private final GradebookService gradebookService;

    @Transactional(readOnly = true)
    public BasicResponse<GradeTargetDTO> calculate(String userId, Double target) {
        if (target == null || target.isNaN() || target < 0 || target > 100) {
            return new BasicResponse<>(false, "יש להזין יעד בין 0 ל-100");
        }
        StudentEntity student = studentRepository.findStudentWithClassRoomByUserId(userId);
        ClassRoomEntity classRoom = student == null ? null : student.getClassRoom();
        if (classRoom == null) {
            return new BasicResponse<>(false, "אינך משויך לכיתה, ולכן אין יומן ציונים לחישוב");
        }

        List<SubjectPlanDTO> subjects = new ArrayList<>();
        List<GradeTargetCalculator.Plan> withData = new ArrayList<>();
        List<TeacherEntity> teachers = new ArrayList<>(classRoom.getTeachers());
        teachers.sort(Comparator.comparing(TeacherEntity::getUsername, Comparator.nullsLast(String::compareTo)));
        for (TeacherEntity teacher : teachers) {
            BasicResponse<GradebookDTO> book = gradebookService.getGradebook(teacher.getUserId(), classRoom.getId());
            if (!book.isSuccess() || book.getData() == null) continue;
            SubjectPlanDTO plan = subjectPlan(teacher, book.getData(), userId, target);
            subjects.add(plan);
            if (!GradeTargetCalculator.Status.NO_DATA.name().equals(plan.status())) {
                withData.add(new GradeTargetCalculator.Plan(GradeTargetCalculator.Status.valueOf(plan.status()),
                        plan.currentGrade(), plan.minPossible(), plan.maxPossible(), plan.requiredPercent()));
            }
        }

        BasicResponse<GradeTargetDTO> response = new BasicResponse<>(true,
                subjects.isEmpty() ? "למורים של הכיתה שלך עדיין אין יומן ציונים" : null);
        response.setData(new GradeTargetDTO(target, subjects, overall(withData, target)));
        return response;
    }

    SubjectPlanDTO subjectPlan(TeacherEntity teacher, GradebookDTO book, String userId, double target) {
        Map<GradeCategory, Integer> weights = new EnumMap<>(GradeCategory.class);
        book.weights().forEach((k, v) -> weights.put(GradeCategory.valueOf(k), v == null ? 0 : v));

        List<GradeTargetCalculator.Column> columns = new ArrayList<>();
        List<GradebookDTO.Column> remainingColumns = new ArrayList<>();
        int graded = 0;
        for (GradebookDTO.Column col : book.columns()) {
            GradeCategory category = GradeCategory.valueOf(col.category());
            Double score = book.grades().getOrDefault(col.key(), Map.of()).get(userId);
            if (score != null && col.maxScore() > 0) {
                columns.add(new GradeTargetCalculator.Column(category, 100.0 * score / col.maxScore()));
                graded++;
            } else if (isRemaining(col)) {
                columns.add(new GradeTargetCalculator.Column(category, null));
                if (weights.getOrDefault(category, 0) > 0) remainingColumns.add(col);
            }
        }
        GradeTargetCalculator.Plan plan = GradeTargetCalculator.plan(columns, weights, target);

        List<RemainingItemDTO> remaining = remainingColumns.stream()
                .map(c -> new RemainingItemDTO(c.title(), c.categoryLabel(), c.maxScore(), c.date(),
                        plan.requiredPercent() == null ? null
                                : Math.ceil(plan.requiredPercent() * c.maxScore() / 10.0 - 1e-9) / 10.0))
                .toList();

        String subjectNames = teacher.getTeachingSubjects() == null ? "" : String.join(", ", teacher.getTeachingSubjects());
        String title = subjectNames.isBlank() ? "יומן ציונים — " + teacher.getUsername() : subjectNames;
        return new SubjectPlanDTO(teacher.getUserId() + "-" + book.classRoomId(), title, teacher.getUsername(),
                plan.current(), plan.min(), plan.max(), plan.requiredPercent(), plan.status().name(),
                message(plan, target, remaining.size()), graded, remaining, book.weights(), book.categoryLabels());
    }

    /** The certificate average: every subject counts the same, and the same x is needed everywhere. */
    static OverallPlanDTO overall(List<GradeTargetCalculator.Plan> plans, double target) {
        if (plans.isEmpty()) return null;
        double min = plans.stream().mapToDouble(GradeTargetCalculator.Plan::min).average().orElse(0);
        double max = plans.stream().mapToDouble(GradeTargetCalculator.Plan::max).average().orElse(0);
        OptionalDouble cur = plans.stream().filter(p -> p.current() != null)
                .mapToDouble(GradeTargetCalculator.Plan::current).average();
        Double current = cur.isPresent() ? cur.getAsDouble() : null;
        GradeTargetCalculator.Plan p;
        if (max - min < 1e-9) {
            GradeTargetCalculator.Status s = min >= target - 1e-9
                    ? GradeTargetCalculator.Status.NO_REMAINING_MET : GradeTargetCalculator.Status.NO_REMAINING_MISSED;
            p = new GradeTargetCalculator.Plan(s, current == null ? null : GradeTargetCalculator.round(current),
                    GradeTargetCalculator.round(min), GradeTargetCalculator.round(max), null);
        } else {
            p = GradeTargetCalculator.solve(current, min, max, target);
        }
        return new OverallPlanDTO(p.current(), p.min(), p.max(), p.requiredPercent(), p.status().name(),
                message(p, target, -1));
    }

    static String message(GradeTargetCalculator.Plan plan, double target, int remainingCount) {
        String t = fmt(target);
        return switch (plan.status()) {
            case POSSIBLE -> remainingCount > 0
                    ? "כדי להגיע ל-" + t + " צריך ממוצע של לפחות " + fmt(plan.requiredPercent())
                      + " בכל אחת מ-" + remainingCount + " המטלות שנשארו"
                    : "כדי להגיע ל-" + t + " צריך ממוצע של לפחות " + fmt(plan.requiredPercent())
                      + " בכל המטלות שנשארו בכל המקצועות";
            case ALREADY_SECURED -> "היעד כבר מובטח: גם עם 0 בכל מה שנשאר, הציון יהיה לפחות " + fmt(plan.min());
            case IMPOSSIBLE -> "היעד " + t + " אינו בר השגה: גם עם 100 בכל מה שנשאר, הציון המרבי יהיה "
                    + fmt(plan.max()) + ". כדאי לבחור יעד נמוך יותר או לדבר עם המורה על עבודה או מועד נוסף";
            case NO_REMAINING_MET -> "אין מטלות שנותרו, והציון הנוכחי " + fmt(plan.current()) + " כבר עומד ביעד";
            case NO_REMAINING_MISSED -> "אין מטלות שנותרו ביומן, והציון הנוכחי " + fmt(plan.current())
                    + " נמוך מהיעד " + t + ". אפשר לבקש מהמורה מטלה נוספת";
            case NO_DATA -> "המורה עדיין לא הזין ציונים או מטלות ביומן הזה";
        };
    }

    /** A teacher's own column without a grade, or a system test that has not happened yet. */
    private static boolean isRemaining(GradebookDTO.Column col) {
        if (GradebookService.MANUAL.equals(col.source())) return true;
        if (col.date() == null) return false;
        try {
            return !LocalDate.parse(col.date(), TEST_DATE).isBefore(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static String fmt(Double value) {
        if (value == null) return "-";
        return value == Math.rint(value) ? String.valueOf(value.longValue()) : String.valueOf(value);
    }
}
