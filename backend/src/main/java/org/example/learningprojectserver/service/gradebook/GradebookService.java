package org.example.learningprojectserver.service.gradebook;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.learningprojectserver.dto.*;
import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.enums.GradeCategory;
import org.example.learningprojectserver.repository.*;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class gradebooks: a teacher's students x assignments table with category weights and a weighted final
 * grade. The teacher's system tests (TeacherTestEntity) appear as read-only exam columns; everything
 * else is entered by hand. Nothing here is cached.
 */
@Service
@RequiredArgsConstructor
public class GradebookService {

    static final String MANUAL = "MANUAL";
    static final String TEACHER_TEST = "TEACHER_TEST";

    private final TeacherRepository teacherRepository;
    private final ClassRoomRepository classRoomRepository;
    private final GradebookAssignmentRepository assignmentRepository;
    private final GradebookGradeRepository gradeRepository;
    private final GradebookWeightRepository weightRepository;

    @Transactional(readOnly = true)
    public BasicResponse<List<GradebookClassDTO>> getClasses(String teacherId) {
        TeacherEntity teacher = teacherRepository.findByUserId(teacherId);
        if (teacher == null) {
            return new BasicResponse<>(false, "המורה לא נמצא");
        }
        List<GradebookClassDTO> classes = teacher.getTeachingClassRooms().stream()
                .map(c -> new GradebookClassDTO(c.getId(), c.getName(),
                        c.getGrade() == null ? null : c.getGrade().getGradeName(), c.getStudents().size()))
                .sorted(Comparator.comparing(GradebookClassDTO::className))
                .toList();
        BasicResponse<List<GradebookClassDTO>> response = new BasicResponse<>(true,
                classes.isEmpty() ? "אינך משויך לאף כיתה" : null);
        response.setData(classes);
        return response;
    }

    @Transactional(readOnly = true)
    public BasicResponse<GradebookDTO> getGradebook(String teacherId, Long classRoomId) {
        ClassRoomEntity classRoom = teacherClass(teacherId, classRoomId);
        if (classRoom == null) {
            return new BasicResponse<>(false, "הכיתה לא נמצאה או שאינך מלמד בה");
        }
        BasicResponse<GradebookDTO> response = new BasicResponse<>(true, null);
        response.setData(build(teacherId, classRoom));
        return response;
    }

    @Transactional
    public BasicResponse<GradebookDTO> addAssignment(String teacherId, GradebookAssignmentRequest request) {
        if (request == null) return new BasicResponse<>(false, "חסרים פרטי המטלה");
        ClassRoomEntity classRoom = teacherClass(teacherId, request.getClassRoomId());
        if (classRoom == null) {
            return new BasicResponse<>(false, "הכיתה לא נמצאה או שאינך מלמד בה");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            return new BasicResponse<>(false, "יש להזין שם למטלה");
        }
        if (request.getTitle().length() > 120) {
            return new BasicResponse<>(false, "שם המטלה ארוך מדי");
        }
        GradeCategory category = parseCategory(request.getCategory());
        if (category == null) {
            return new BasicResponse<>(false, "סוג המטלה אינו תקין");
        }
        double maxScore = request.getMaxScore() == null ? 100 : request.getMaxScore();
        if (maxScore <= 0 || maxScore > 1000) {
            return new BasicResponse<>(false, "הניקוד המרבי חייב להיות בין 1 ל-1000");
        }
        LocalDate date = LocalDate.now();
        if (request.getDate() != null && !request.getDate().isBlank()) {
            try {
                date = LocalDate.parse(request.getDate());
            } catch (DateTimeParseException e) {
                return new BasicResponse<>(false, "התאריך אינו תקין");
            }
        }

        GradebookAssignmentEntity assignment = new GradebookAssignmentEntity();
        assignment.setTeacherId(teacherId);
        assignment.setClassRoomId(classRoom.getId());
        assignment.setTitle(request.getTitle().trim());
        assignment.setCategory(category);
        assignment.setMaxScore(maxScore);
        assignment.setAssignedDate(date);
        assignmentRepository.save(assignment);

        BasicResponse<GradebookDTO> response = new BasicResponse<>(true, "המטלה נוספה ליומן הציונים");
        response.setData(build(teacherId, classRoom));
        return response;
    }

    @Transactional
    public BasicResponse<GradebookDTO> deleteAssignment(String teacherId, Long assignmentId) {
        GradebookAssignmentEntity assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null || !assignment.getTeacherId().equals(teacherId)) {
            return new BasicResponse<>(false, "המטלה לא נמצאה");
        }
        ClassRoomEntity classRoom = teacherClass(teacherId, assignment.getClassRoomId());
        gradeRepository.deleteByAssignmentId(assignmentId);
        assignmentRepository.delete(assignment);

        BasicResponse<GradebookDTO> response = new BasicResponse<>(true, "המטלה והציונים שלה נמחקו");
        if (classRoom != null) response.setData(build(teacherId, classRoom));
        return response;
    }

    /** score null or empty clears the grade. */
    @Transactional
    public BasicResponse<Void> setGrade(String teacherId, Long assignmentId, String studentId, Double score) {
        GradebookAssignmentEntity assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null || !assignment.getTeacherId().equals(teacherId)) {
            return new BasicResponse<>(false, "המטלה לא נמצאה");
        }
        ClassRoomEntity classRoom = teacherClass(teacherId, assignment.getClassRoomId());
        if (classRoom == null) {
            return new BasicResponse<>(false, "הכיתה לא נמצאה או שאינך מלמד בה");
        }
        boolean inClass = classRoom.getStudents().stream().anyMatch(s -> s.getUserId().equals(studentId));
        if (!inClass) {
            return new BasicResponse<>(false, "התלמיד אינו לומד בכיתה זו");
        }

        Optional<GradebookGradeEntity> existing = gradeRepository.findByAssignmentAndStudentUserId(assignment, studentId);
        if (score == null) {
            existing.ifPresent(gradeRepository::delete);
            return new BasicResponse<>(true, "הציון נמחק");
        }
        if (score < 0 || score > assignment.getMaxScore()) {
            return new BasicResponse<>(false, "הציון חייב להיות בין 0 ל-" + formatNumber(assignment.getMaxScore()));
        }
        GradebookGradeEntity grade = existing.orElseGet(() -> {
            GradebookGradeEntity g = new GradebookGradeEntity();
            g.setAssignment(assignment);
            g.setStudentUserId(studentId);
            return g;
        });
        grade.setScore(score);
        gradeRepository.save(grade);
        return new BasicResponse<>(true, "הציון נשמר");
    }

    @Transactional
    public BasicResponse<GradebookDTO> setWeights(String teacherId, Long classRoomId, Map<String, Integer> weights) {
        ClassRoomEntity classRoom = teacherClass(teacherId, classRoomId);
        if (classRoom == null) {
            return new BasicResponse<>(false, "הכיתה לא נמצאה או שאינך מלמד בה");
        }
        if (weights == null || weights.isEmpty()) {
            return new BasicResponse<>(false, "לא הוזנו משקלים");
        }
        Map<GradeCategory, Integer> parsed = new EnumMap<>(GradeCategory.class);
        for (Map.Entry<String, Integer> e : weights.entrySet()) {
            GradeCategory category = parseCategory(e.getKey());
            Integer weight = e.getValue();
            if (category == null) return new BasicResponse<>(false, "סוג מטלה לא מוכר: " + e.getKey());
            if (weight == null || weight < 0 || weight > 100) {
                return new BasicResponse<>(false, "כל משקל חייב להיות בין 0 ל-100");
            }
            parsed.put(category, weight);
        }
        Map<GradeCategory, Integer> merged = currentWeights(teacherId, classRoomId);
        merged.putAll(parsed);
        int total = merged.values().stream().mapToInt(Integer::intValue).sum();
        if (total != 100) {
            return new BasicResponse<>(false, "סכום המשקלים חייב להיות 100% (כעת " + total + "%)");
        }

        Map<GradeCategory, GradebookWeightEntity> rows = weightRepository.findByTeacherIdAndClassRoomId(teacherId, classRoomId)
                .stream().collect(Collectors.toMap(GradebookWeightEntity::getCategory, w -> w));
        for (Map.Entry<GradeCategory, Integer> e : merged.entrySet()) {
            GradebookWeightEntity row = rows.get(e.getKey());
            if (row == null) {
                row = new GradebookWeightEntity();
                row.setTeacherId(teacherId);
                row.setClassRoomId(classRoomId);
                row.setCategory(e.getKey());
            }
            row.setWeight(e.getValue());
            weightRepository.save(row);
        }

        BasicResponse<GradebookDTO> response = new BasicResponse<>(true, "המשקלים נשמרו");
        response.setData(build(teacherId, classRoom));
        return response;
    }

    /** The gradebook as an .xlsx file, or null when the teacher does not teach the class. */
    @Transactional(readOnly = true)
    public byte[] exportExcel(String teacherId, Long classRoomId) throws IOException {
        ClassRoomEntity classRoom = teacherClass(teacherId, classRoomId);
        if (classRoom == null) return null;
        return GradebookExcelWriter.write(build(teacherId, classRoom));
    }

    public String exportFileName(String teacherId, Long classRoomId) {
        return "gradebook-" + classRoomId + "-" + LocalDate.now() + ".xlsx";
    }

    private GradebookDTO build(String teacherId, ClassRoomEntity classRoom) {
        Long classRoomId = classRoom.getId();
        List<GradebookDTO.Student> students = classRoom.getStudents().stream()
                .map(s -> new GradebookDTO.Student(s.getUserId(), s.getUsername()))
                .sorted(Comparator.comparing(GradebookDTO.Student::name, Comparator.nullsLast(String::compareTo)))
                .toList();
        Set<String> studentIds = students.stream().map(GradebookDTO.Student::userId).collect(Collectors.toSet());

        List<GradebookDTO.Column> columns = new ArrayList<>();
        List<GradebookCalculator.ColumnSpec> specs = new ArrayList<>();
        Map<String, Map<String, Double>> grades = new LinkedHashMap<>();

        // System tests the teacher gave this class: read-only exam columns, best score per student.
        List<TeacherTestColumnDTO> tests = assignmentRepository.findTeacherTestsForClass(teacherId, classRoomId);
        if (!tests.isEmpty()) {
            Map<Long, Map<String, Double>> testScores = new HashMap<>();
            for (TeacherTestScoreDTO s : assignmentRepository.findTeacherTestScores(
                    tests.stream().map(TeacherTestColumnDTO::testId).toList())) {
                if (!studentIds.contains(s.studentUserId())) continue;
                testScores.computeIfAbsent(s.testId(), k -> new HashMap<>())
                        .merge(s.studentUserId(), (double) s.score(), Math::max);
            }
            for (TeacherTestColumnDTO t : tests) {
                String key = "t-" + t.testId();
                String title = "מבחן: " + Objects.toString(t.topic(), Objects.toString(t.subject(), ""));
                String date = t.startTime() == null ? null : t.startTime().split(" ")[0];
                columns.add(new GradebookDTO.Column(key, null, t.testId(), title, GradeCategory.EXAM.name(),
                        GradeCategory.EXAM.getLabel(), 100, date, TEACHER_TEST, false));
                specs.add(new GradebookCalculator.ColumnSpec(key, GradeCategory.EXAM, 100));
                grades.put(key, testScores.getOrDefault(t.testId(), new HashMap<>()));
            }
        }

        List<GradebookAssignmentEntity> assignments =
                assignmentRepository.findByTeacherIdAndClassRoomIdOrderByAssignedDateAscIdAsc(teacherId, classRoomId);
        Map<Long, List<GradebookGradeEntity>> gradesByAssignment = assignments.isEmpty() ? Map.of()
                : gradeRepository.findByAssignmentIds(assignments.stream().map(GradebookAssignmentEntity::getId).toList())
                .stream().collect(Collectors.groupingBy(g -> g.getAssignment().getId()));
        for (GradebookAssignmentEntity a : assignments) {
            String key = "a-" + a.getId();
            columns.add(new GradebookDTO.Column(key, a.getId(), null, a.getTitle(), a.getCategory().name(),
                    a.getCategory().getLabel(), a.getMaxScore(),
                    a.getAssignedDate() == null ? null : a.getAssignedDate().toString(), MANUAL, true));
            specs.add(new GradebookCalculator.ColumnSpec(key, a.getCategory(), a.getMaxScore()));
            Map<String, Double> scores = new HashMap<>();
            for (GradebookGradeEntity g : gradesByAssignment.getOrDefault(a.getId(), List.of())) {
                if (studentIds.contains(g.getStudentUserId())) scores.put(g.getStudentUserId(), g.getScore());
            }
            grades.put(key, scores);
        }

        Map<GradeCategory, Integer> weights = currentWeights(teacherId, classRoomId);
        Map<String, GradebookCalculator.StudentSummary> summaries = GradebookCalculator.compute(
                students.stream().map(GradebookDTO.Student::userId).toList(), specs, grades, weights);

        Map<String, Map<String, Double>> categoryAverages = new LinkedHashMap<>();
        Map<String, Double> finals = new LinkedHashMap<>();
        summaries.forEach((id, s) -> {
            categoryAverages.put(id, s.categoryAverages());
            finals.put(id, s.finalGrade());
        });

        Map<String, Integer> weightsOut = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        for (GradeCategory c : GradeCategory.values()) {
            weightsOut.put(c.name(), weights.get(c));
            labels.put(c.name(), c.getLabel());
        }

        return new GradebookDTO(classRoomId, classRoom.getName(), students, columns, grades, weightsOut, labels,
                categoryAverages, finals, GradebookCalculator.average(finals.values()));
    }

    private Map<GradeCategory, Integer> currentWeights(String teacherId, Long classRoomId) {
        Map<GradeCategory, Integer> weights = new EnumMap<>(GradeCategory.class);
        for (GradeCategory c : GradeCategory.values()) weights.put(c, c.getDefaultWeight());
        for (GradebookWeightEntity w : weightRepository.findByTeacherIdAndClassRoomId(teacherId, classRoomId)) {
            weights.put(w.getCategory(), w.getWeight());
        }
        return weights;
    }

    /** The class, if the teacher teaches it; null otherwise. */
    private ClassRoomEntity teacherClass(String teacherId, Long classRoomId) {
        if (classRoomId == null) return null;
        TeacherEntity teacher = teacherRepository.findByUserId(teacherId);
        if (teacher == null) return null;
        return teacher.getTeachingClassRooms().stream()
                .filter(c -> c.getId().equals(classRoomId))
                .findFirst()
                .orElse(null);
    }

    private static GradeCategory parseCategory(String value) {
        if (value == null) return null;
        try {
            return GradeCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static String formatNumber(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    /** Writes a gradebook to a right-to-left .xlsx sheet. */
    static final class GradebookExcelWriter {
        private GradebookExcelWriter() {}

        static byte[] write(GradebookDTO book) throws IOException {
            try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet(safeSheetName(book.className()));
                sheet.setRightToLeft(true);

                CellStyle header = workbook.createCellStyle();
                Font bold = workbook.createFont();
                bold.setBold(true);
                header.setFont(bold);
                header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                header.setWrapText(true);
                CellStyle number = workbook.createCellStyle();
                number.setDataFormat(workbook.createDataFormat().getFormat("0.0"));

                List<String> categories = new ArrayList<>(book.categoryLabels().keySet());

                int r = 0;
                Row title = sheet.createRow(r++);
                title.createCell(0).setCellValue("יומן ציונים — כיתה " + book.className());
                title.getCell(0).setCellStyle(header);

                Row head = sheet.createRow(r++);
                int c = 0;
                head.createCell(c++).setCellValue("שם התלמיד");
                head.createCell(c++).setCellValue("מספר זהות");
                for (GradebookDTO.Column col : book.columns()) {
                    head.createCell(c++).setCellValue(col.title() + " (" + col.categoryLabel() + ", מתוך "
                            + formatNumber(col.maxScore()) + ")");
                }
                for (String cat : categories) {
                    head.createCell(c++).setCellValue("ממוצע " + book.categoryLabels().get(cat)
                            + " (" + book.weights().get(cat) + "%)");
                }
                head.createCell(c).setCellValue("ציון משוקלל");
                for (Cell cell : head) cell.setCellStyle(header);

                for (GradebookDTO.Student s : book.students()) {
                    Row row = sheet.createRow(r++);
                    c = 0;
                    row.createCell(c++).setCellValue(s.name());
                    row.createCell(c++).setCellValue(s.userId());
                    for (GradebookDTO.Column col : book.columns()) {
                        Double score = book.grades().getOrDefault(col.key(), Map.of()).get(s.userId());
                        Cell cell = row.createCell(c++);
                        if (score != null) cell.setCellValue(score);
                    }
                    Map<String, Double> averages = book.categoryAverages().getOrDefault(s.userId(), Map.of());
                    for (String cat : categories) {
                        Cell cell = row.createCell(c++);
                        Double avg = averages.get(cat);
                        if (avg != null) {
                            cell.setCellValue(avg);
                            cell.setCellStyle(number);
                        }
                    }
                    Cell fin = row.createCell(c);
                    Double finalGrade = book.finalGrades().get(s.userId());
                    if (finalGrade != null) {
                        fin.setCellValue(finalGrade);
                        fin.setCellStyle(number);
                    }
                }

                Row avgRow = sheet.createRow(r);
                avgRow.createCell(0).setCellValue("ממוצע כיתתי");
                avgRow.getCell(0).setCellStyle(header);
                int lastCol = 2 + book.columns().size() + categories.size();
                if (book.classAverage() != null) {
                    Cell cell = avgRow.createCell(lastCol);
                    cell.setCellValue(book.classAverage());
                    cell.setCellStyle(number);
                }

                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, Math.max(1, lastCol)));
                sheet.createFreezePane(2, 2);
                sheet.setColumnWidth(0, 24 * 256);
                sheet.setColumnWidth(1, 14 * 256);
                for (int i = 2; i <= lastCol; i++) sheet.setColumnWidth(i, 16 * 256);

                workbook.write(out);
                return out.toByteArray();
            }
        }

        private static String safeSheetName(String name) {
            String cleaned = Objects.toString(name, "gradebook").replaceAll("[\\\\/?*\\[\\]:]", "-");
            return cleaned.isBlank() ? "gradebook" : cleaned.substring(0, Math.min(31, cleaned.length()));
        }
    }
}
