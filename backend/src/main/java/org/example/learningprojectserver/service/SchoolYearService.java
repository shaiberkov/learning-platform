package org.example.learningprojectserver.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.*;
import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.repository.*;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.utils.GradeLevels;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * School years and semesters, and the year close: archive a snapshot of every student,
 * then promote all students to the next grade (or graduate them) in one transaction.
 */
@Service
@RequiredArgsConstructor
public class SchoolYearService {

    static final String PROMOTED = "PROMOTED";
    static final String GRADUATED = "GRADUATED";
    static final String STAYED = "STAYED";
    private static final DateTimeFormatter TEST_START_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SchoolRepository schoolRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final ArchivedStudentRecordRepository archivedStudentRecordRepository;
    private final SchoolAnalyticsRepository schoolAnalyticsRepository;
    private final SchoolCacheEvictor cacheEvictor;

    @Transactional
    public BasicResponse getYears(String schoolCode) {
        if (schoolRepository.findBySchoolCode(schoolCode) == null) {
            return new BasicResponse(false, "בית הספר לא נמצא");
        }
        List<SchoolYearDTO> years = schoolYearRepository.findBySchoolCode(schoolCode).stream().map(this::toDTO).toList();
        BasicResponse<List<SchoolYearDTO>> response = new BasicResponse<>(true, null);
        response.setData(years);
        return response;
    }

    @Transactional
    public BasicResponse createYear(String schoolCode, String name, String startDate, String endDate) {
        SchoolEntity school = schoolRepository.findBySchoolCode(schoolCode);
        if (school == null) {
            return new BasicResponse(false, "בית הספר לא נמצא");
        }
        if (name == null || name.isBlank()) {
            return new BasicResponse(false, "יש להזין שם לשנת הלימודים");
        }
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        if (start == null || end == null || !end.isAfter(start)) {
            return new BasicResponse(false, "יש להזין תאריך התחלה ותאריך סיום מאוחר ממנו");
        }
        List<SchoolYearEntity> existing = schoolYearRepository.findBySchoolCode(schoolCode);
        if (existing.stream().anyMatch(y -> y.getName().equalsIgnoreCase(name.trim()))) {
            return new BasicResponse(false, "שנת לימודים בשם זה כבר קיימת");
        }
        SchoolYearEntity year = newYear(school, name.trim(), start, end, existing.stream().noneMatch(SchoolYearEntity::isActive));
        schoolYearRepository.save(year);
        return new BasicResponse(true, "שנת הלימודים " + year.getName() + " נוצרה");
    }

    @Transactional
    public BasicResponse addSemester(String schoolCode, Long yearId, String name, String startDate, String endDate) {
        SchoolYearEntity year = schoolYearRepository.findByIdAndSchoolCode(yearId, schoolCode);
        if (year == null) {
            return new BasicResponse(false, "שנת הלימודים לא נמצאה");
        }
        if (year.isClosed()) {
            return new BasicResponse(false, "לא ניתן לשנות שנה שנסגרה");
        }
        if (name == null || name.isBlank()) {
            return new BasicResponse(false, "יש להזין שם למחצית");
        }
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        if (start == null || end == null || !end.isAfter(start)) {
            return new BasicResponse(false, "יש להזין תאריך התחלה ותאריך סיום מאוחר ממנו");
        }
        if ((year.getStartDate() != null && start.isBefore(year.getStartDate()))
                || (year.getEndDate() != null && end.isAfter(year.getEndDate()))) {
            return new BasicResponse(false, "המחצית חייבת להיות בתוך תאריכי שנת הלימודים");
        }
        for (SemesterEntity other : year.getSemesters()) {
            if (start.isBefore(other.getEndDate()) && end.isAfter(other.getStartDate())) {
                return new BasicResponse(false, "המחצית חופפת למחצית " + other.getName());
            }
        }
        SemesterEntity semester = new SemesterEntity();
        semester.setName(name.trim());
        semester.setStartDate(start);
        semester.setEndDate(end);
        semester.setSchoolYear(year);
        year.getSemesters().add(semester);
        schoolYearRepository.save(year);
        return new BasicResponse(true, "המחצית " + semester.getName() + " נוספה");
    }

    @Transactional
    public BasicResponse removeSemester(String schoolCode, Long yearId, Long semesterId) {
        SchoolYearEntity year = schoolYearRepository.findByIdAndSchoolCode(yearId, schoolCode);
        if (year == null) {
            return new BasicResponse(false, "שנת הלימודים לא נמצאה");
        }
        if (year.isClosed()) {
            return new BasicResponse(false, "לא ניתן לשנות שנה שנסגרה");
        }
        boolean removed = year.getSemesters().removeIf(s -> s.getId().equals(semesterId));
        return removed ? new BasicResponse(true, "המחצית הוסרה") : new BasicResponse(false, "המחצית לא נמצאה");
    }

    /** The default promotion plan: every grade moves to the next level, the top grade graduates. */
    @Transactional
    public BasicResponse getPromotionPlan(String schoolCode) {
        SchoolEntity school = schoolRepository.findBySchoolCode(schoolCode);
        if (school == null) {
            return new BasicResponse(false, "בית הספר לא נמצא");
        }
        List<GradePromotionDTO> plan = defaultPlan(gradeNames(school));
        Map<String, Integer> counts = studentCountByGrade(school);
        plan.forEach(p -> p.setStudentCount(counts.getOrDefault(p.getFromGrade(), 0)));
        BasicResponse<List<GradePromotionDTO>> response = new BasicResponse<>(true, null);
        response.setData(plan);
        return response;
    }

    /**
     * Closes a year: archives every student in a class, moves them per the plan (or the default plan),
     * marks the year closed and optionally opens the next one. One transaction: all or nothing.
     */
    @Transactional
    public BasicResponse closeYearAndPromote(String schoolCode, Long yearId, List<GradePromotionDTO> requestedPlan, String nextYearName) {
        SchoolEntity school = schoolRepository.findBySchoolCode(schoolCode);
        if (school == null) {
            return new BasicResponse(false, "בית הספר לא נמצא");
        }
        SchoolYearEntity year = schoolYearRepository.findByIdAndSchoolCode(yearId, schoolCode);
        if (year == null) {
            return new BasicResponse(false, "שנת הלימודים לא נמצאה");
        }
        if (year.isClosed()) {
            return new BasicResponse(false, "שנת הלימודים כבר נסגרה");
        }

        Map<String, SchoolGradeEntity> gradesByName = school.getSchoolGrades().stream()
                .collect(Collectors.toMap(SchoolGradeEntity::getGradeName, g -> g, (a, b) -> a));
        List<GradePromotionDTO> plan = requestedPlan == null || requestedPlan.isEmpty()
                ? defaultPlan(new ArrayList<>(gradesByName.keySet()))
                : requestedPlan;
        Map<String, String> target = new HashMap<>();
        for (GradePromotionDTO line : plan) {
            if (!gradesByName.containsKey(line.getFromGrade())) {
                return new BasicResponse(false, "השכבה " + line.getFromGrade() + " לא קיימת בבית הספר");
            }
            String to = line.getToGrade() == null ? "" : line.getToGrade().trim();
            if (!to.isEmpty() && !gradesByName.containsKey(to)) {
                return new BasicResponse(false, "שכבת היעד " + to + " לא קיימת בבית הספר");
            }
            target.put(line.getFromGrade(), to);
        }

        Map<String, List<Integer>> scoresByStudent = yearScores(schoolCode, year);

        // Plan every move from the current snapshot first, so a student is never moved twice.
        record Move(StudentEntity student, ClassRoomEntity from, ClassRoomEntity to, String outcome, String toGrade) {}
        List<Move> moves = new ArrayList<>();
        for (SchoolGradeEntity grade : school.getSchoolGrades()) {
            String toGradeName = target.getOrDefault(grade.getGradeName(), grade.getGradeName());
            for (ClassRoomEntity classRoom : grade.getClasses()) {
                for (StudentEntity student : new ArrayList<>(classRoom.getStudents())) {
                    if (toGradeName.isEmpty()) {
                        moves.add(new Move(student, classRoom, null, GRADUATED, null));
                    } else if (toGradeName.equals(grade.getGradeName())) {
                        moves.add(new Move(student, classRoom, classRoom, STAYED, grade.getGradeName()));
                    } else {
                        SchoolGradeEntity toGrade = gradesByName.get(toGradeName);
                        moves.add(new Move(student, classRoom,
                                targetClass(classRoom.getName(), grade.getGradeName(), toGrade), PROMOTED, toGradeName));
                    }
                }
            }
        }

        int promoted = 0, graduated = 0, stayed = 0;
        for (Move move : moves) {
            StudentEntity student = move.student();
            List<Integer> scores = scoresByStudent.getOrDefault(student.getUserId(), List.of());

            ArchivedStudentRecordEntity record = new ArchivedStudentRecordEntity();
            record.setSchoolYear(year);
            record.setStudentUserId(student.getUserId());
            record.setStudentName(student.getUsername());
            record.setGradeName(move.from().getGrade().getGradeName());
            record.setClassName(move.from().getName());
            record.setTestsCount(scores.size());
            record.setAverageScore(scores.isEmpty() ? null
                    : SchoolAnalyticsService.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0)));
            record.setOutcome(move.outcome());
            record.setNextGradeName(move.toGrade());
            record.setNextClassName(move.to() == null ? null : move.to().getName());
            archivedStudentRecordRepository.save(record);

            switch (move.outcome()) {
                case STAYED -> stayed++;
                case GRADUATED -> {
                    detach(student, move.from());
                    student.setSchoolName(null);
                    graduated++;
                }
                default -> {
                    detach(student, move.from());
                    if (move.to() != null) {
                        attach(student, move.to());
                    }
                    promoted++;
                }
            }
            cacheEvictor.studentSchedule(student.getUserId());
        }

        year.setClosed(true);
        year.setActive(false);
        year.setClosedAt(LocalDateTime.now());
        schoolYearRepository.save(year);

        String message = "שנת הלימודים " + year.getName() + " נסגרה. עלו כיתה: " + promoted
                + ", סיימו את בית הספר: " + graduated + ", נשארו: " + stayed;
        if (nextYearName != null && !nextYearName.isBlank()) {
            LocalDate start = year.getEndDate() != null ? year.getEndDate().plusDays(1) : LocalDate.now();
            SchoolYearEntity next = newYear(school, nextYearName.trim(), start, start.plusYears(1).minusDays(1), true);
            schoolYearRepository.save(next);
            message += ". נפתחה שנת הלימודים " + next.getName();
        }
        if (school.getSchoolManager() != null) {
            cacheEvictor.schoolDTO(school.getSchoolManager().getUserId());
        }
        return new BasicResponse(true, message);
    }

    public BasicResponse getArchive(String schoolCode, Long yearId) {
        SchoolYearEntity year = schoolYearRepository.findByIdAndSchoolCode(yearId, schoolCode);
        if (year == null) {
            return new BasicResponse(false, "שנת הלימודים לא נמצאה");
        }
        List<ArchivedStudentRecordDTO> records = archivedStudentRecordRepository
                .findBySchoolYear_IdOrderByGradeNameAscClassNameAscStudentNameAsc(yearId).stream()
                .map(r -> new ArchivedStudentRecordDTO(r.getStudentUserId(), r.getStudentName(), r.getGradeName(),
                        r.getClassName(), r.getTestsCount(), r.getAverageScore(), r.getOutcome(),
                        r.getNextGradeName(), r.getNextClassName()))
                .toList();
        BasicResponse<List<ArchivedStudentRecordDTO>> response = new BasicResponse<>(true,
                records.isEmpty() ? "אין נתוני ארכיון לשנה זו" : null);
        response.setData(records);
        return response;
    }

    private void detach(StudentEntity student, ClassRoomEntity from) {
        from.getStudents().remove(student);
        for (TeacherEntity teacher : from.getTeachers()) {
            teacher.getStudents().remove(student);
            student.getTeachers().remove(teacher);
        }
        student.setClassRoom(null);
    }

    private void attach(StudentEntity student, ClassRoomEntity to) {
        student.setClassRoom(to);
        to.getStudents().add(student);
        for (TeacherEntity teacher : to.getTeachers()) {
            if (!teacher.getStudents().contains(student)) teacher.getStudents().add(student);
            if (!student.getTeachers().contains(teacher)) student.getTeachers().add(teacher);
        }
    }

    /** Teacher-test scores per student for tests that started inside the year's dates. */
    private Map<String, List<Integer>> yearScores(String schoolCode, SchoolYearEntity year) {
        Map<String, List<Integer>> scores = new HashMap<>();
        for (GradeRecordDTO r : schoolAnalyticsRepository.findGradeRecordsBySchoolCode(schoolCode)) {
            if (inYear(r.getTestStartTime(), year)) {
                scores.computeIfAbsent(r.getStudentUserId(), k -> new ArrayList<>()).add(r.getScore());
            }
        }
        return scores;
    }

    static boolean inYear(String testStartTime, SchoolYearEntity year) {
        if (year.getStartDate() == null || year.getEndDate() == null) return true;
        try {
            LocalDate date = LocalDateTime.parse(testStartTime.trim(), TEST_START_FORMAT).toLocalDate();
            return !date.isBefore(year.getStartDate()) && !date.isAfter(year.getEndDate());
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * The class a student moves to: the class with the same suffix in the target grade
     * ("ז2" → "ח2"), else the first class of the target grade by name, else none.
     */
    static ClassRoomEntity targetClass(String className, String fromGrade, SchoolGradeEntity toGrade) {
        if (toGrade == null || toGrade.getClasses().isEmpty()) return null;
        String suffix = className.startsWith(fromGrade) ? className.substring(fromGrade.length()) : className;
        String wanted = toGrade.getGradeName() + suffix;
        return toGrade.getClasses().stream()
                .filter(c -> c.getName().equals(wanted))
                .findFirst()
                .orElseGet(() -> toGrade.getClasses().stream()
                        .min(Comparator.comparing(ClassRoomEntity::getName)).orElse(null));
    }

    /** Each grade moves to the grade one level above it; the top grade graduates; unrecognised names stay. */
    static List<GradePromotionDTO> defaultPlan(List<String> gradeNames) {
        Map<Integer, String> byLevel = new HashMap<>();
        for (String name : gradeNames) {
            int level = GradeLevels.level(name);
            if (level > 0) byLevel.putIfAbsent(level, name);
        }
        int top = byLevel.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
        List<GradePromotionDTO> plan = new ArrayList<>();
        for (String name : gradeNames) {
            int level = GradeLevels.level(name);
            String to;
            if (level <= 0) to = name;
            else if (byLevel.containsKey(level + 1)) to = byLevel.get(level + 1);
            else if (level == top) to = "";
            else to = name; // a gap in the levels: keep the grade rather than guess
            plan.add(new GradePromotionDTO(name, to, 0));
        }
        plan.sort(Comparator.comparingInt((GradePromotionDTO p) -> GradeLevels.level(p.getFromGrade()))
                .thenComparing(GradePromotionDTO::getFromGrade));
        return plan;
    }

    private static List<String> gradeNames(SchoolEntity school) {
        return school.getSchoolGrades().stream().map(SchoolGradeEntity::getGradeName).toList();
    }

    private static Map<String, Integer> studentCountByGrade(SchoolEntity school) {
        Map<String, Integer> counts = new HashMap<>();
        for (SchoolGradeEntity grade : school.getSchoolGrades()) {
            counts.put(grade.getGradeName(), grade.getClasses().stream().mapToInt(c -> c.getStudents().size()).sum());
        }
        return counts;
    }

    private SchoolYearEntity newYear(SchoolEntity school, String name, LocalDate start, LocalDate end, boolean active) {
        SchoolYearEntity year = new SchoolYearEntity();
        year.setSchool(school);
        year.setName(name);
        year.setStartDate(start);
        year.setEndDate(end);
        year.setActive(active);
        return year;
    }

    private SchoolYearDTO toDTO(SchoolYearEntity year) {
        SchoolYearDTO dto = new SchoolYearDTO();
        dto.setId(year.getId());
        dto.setName(year.getName());
        dto.setStartDate(year.getStartDate() == null ? null : year.getStartDate().toString());
        dto.setEndDate(year.getEndDate() == null ? null : year.getEndDate().toString());
        dto.setActive(year.isActive());
        dto.setClosed(year.isClosed());
        dto.setClosedAt(year.getClosedAt() == null ? null
                : year.getClosedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        dto.setArchivedStudents(year.isClosed() ? archivedStudentRecordRepository.countBySchoolYear_Id(year.getId()) : 0);
        dto.setSemesters(year.getSemesters().stream()
                .map(s -> new SemesterDTO(s.getId(), s.getName(), s.getStartDate().toString(), s.getEndDate().toString()))
                .toList());
        return dto;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
