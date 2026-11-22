package org.example.learningprojectserver.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.BulkImportCreatedUserDTO;
import org.example.learningprojectserver.dto.BulkImportPreviewDTO;
import org.example.learningprojectserver.dto.BulkImportRowDTO;
import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.repository.ClassRoomRepository;
import org.example.learningprojectserver.repository.SchoolRepository;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

import static org.example.learningprojectserver.utils.GeneratorUtils.*;

/**
 * Bulk user import from CSV: parse, validate every row (preview), then create the valid rows
 * with a generated initial password (commit). Commit re-validates the same file, so it is stateless.
 */
@Service
@RequiredArgsConstructor
public class BulkImportService {

    static final int MAX_ROWS = 1000;
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Canonical column name → accepted header spellings (lower-case). */
    private static final Map<String, List<String>> HEADER_ALIASES = Map.of(
            "role", List.of("role", "תפקיד"),
            "username", List.of("username", "name", "שם", "שם מלא"),
            "userId", List.of("userid", "id", "תעודת זהות", "ת.ז", "תז", "ת\"ז"),
            "email", List.of("email", "אימייל", "מייל", "דוא\"ל"),
            "phone", List.of("phone", "טלפון", "נייד"),
            "className", List.of("classname", "class", "כיתה", "כיתות"),
            "subjects", List.of("subjects", "מקצועות"),
            "children", List.of("children", "ילדים", "ת.ז ילדים"));
    private static final List<String> REQUIRED_COLUMNS = List.of("role", "username", "userId", "email", "phone");

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ClassRoomRepository classRoomRepository;
    private final SchoolCacheEvictor cacheEvictor;

    public BasicResponse preview(String schoolCode, String csv) {
        SchoolEntity school = schoolRepository.findBySchoolCode(schoolCode);
        if (school == null) {
            return new BasicResponse(false, "בית הספר לא נמצא");
        }
        BulkImportPreviewDTO preview = validate(schoolCode, csv);
        BasicResponse<BulkImportPreviewDTO> response = new BasicResponse<>(preview.getFileErrors().isEmpty(),
                preview.getFileErrors().isEmpty() ? null : String.join(". ", preview.getFileErrors()));
        response.setData(preview);
        return response;
    }

    @Transactional
    public BasicResponse commit(String schoolCode, String csv) {
        SchoolEntity school = schoolRepository.findBySchoolCode(schoolCode);
        if (school == null) {
            return new BasicResponse(false, "בית הספר לא נמצא");
        }
        BulkImportPreviewDTO preview = validate(schoolCode, csv);
        if (!preview.getFileErrors().isEmpty()) {
            return new BasicResponse(false, String.join(". ", preview.getFileErrors()));
        }
        List<BulkImportRowDTO> valid = preview.getRows().stream().filter(BulkImportRowDTO::isValid).toList();
        if (valid.isEmpty()) {
            return new BasicResponse(false, "אין שורות תקינות לייבוא");
        }

        List<BulkImportCreatedUserDTO> created = new ArrayList<>();
        Map<String, StudentEntity> studentsById = new HashMap<>();

        for (BulkImportRowDTO row : valid) {
            if (!Role.STUDENT.name().equals(row.getRole())) continue;
            String password = generateInitialPassword();
            StudentEntity student = new StudentEntity();
            fillUser(student, row, Role.STUDENT, password);
            StudentProgressEntity progress = new StudentProgressEntity();
            StudentQuestionHistoryEntity history = new StudentQuestionHistoryEntity();
            progress.setStudent(student);
            history.setStudent(student);
            student.setStudentProgressEntity(progress);
            student.setQuestionHistoryEntity(history);

            ClassRoomEntity classRoom = classRoomRepository.findBySchoolCodeAndClassName(schoolCode, row.getClassName());
            student.setSchoolName(school);
            student.setClassRoom(classRoom);
            classRoom.getStudents().add(student);
            for (TeacherEntity teacher : classRoom.getTeachers()) {
                teacher.getStudents().add(student);
                student.getTeachers().add(teacher);
            }
            userRepository.save(student);
            studentsById.put(student.getUserId(), student);
            created.add(new BulkImportCreatedUserDTO(row.getRowNumber(), row.getRole(), row.getUsername(), row.getUserId(), password));
        }

        for (BulkImportRowDTO row : valid) {
            if (!Role.TEACHER.name().equals(row.getRole())) continue;
            String password = generateInitialPassword();
            TeacherEntity teacher = new TeacherEntity();
            fillUser(teacher, row, Role.TEACHER, password);
            teacher.setTeachingSchool(school);
            teacher.getTeachingSubjects().addAll(row.getSubjects());
            school.getTeachers().add(teacher);
            for (String className : splitList(row.getClassName())) {
                ClassRoomEntity classRoom = classRoomRepository.findBySchoolCodeAndClassName(schoolCode, className);
                classRoom.getTeachers().add(teacher);
                teacher.getTeachingClassRooms().add(classRoom);
                for (StudentEntity student : classRoom.getStudents()) {
                    teacher.getStudents().add(student);
                    student.getTeachers().add(teacher);
                }
            }
            userRepository.save(teacher);
            created.add(new BulkImportCreatedUserDTO(row.getRowNumber(), row.getRole(), row.getUsername(), row.getUserId(), password));
        }

        for (BulkImportRowDTO row : valid) {
            if (!Role.PARENT.name().equals(row.getRole())) continue;
            String password = generateInitialPassword();
            ParentEntity parent = new ParentEntity();
            fillUser(parent, row, Role.PARENT, password);
            for (String childId : row.getChildren()) {
                StudentEntity child = studentsById.get(childId);
                if (child == null) {
                    child = studentRepository.findStudentByStudentId(childId);
                }
                parent.getChildren().add(child);
            }
            userRepository.save(parent);
            created.add(new BulkImportCreatedUserDTO(row.getRowNumber(), row.getRole(), row.getUsername(), row.getUserId(), password));
        }

        if (school.getSchoolManager() != null) {
            cacheEvictor.schoolDTO(school.getSchoolManager().getUserId());
        }

        created.sort(Comparator.comparingInt(BulkImportCreatedUserDTO::getRowNumber));
        int skipped = preview.getRows().size() - valid.size();
        BasicResponse<List<BulkImportCreatedUserDTO>> response = new BasicResponse<>(true,
                "נוצרו " + created.size() + " משתמשים" + (skipped > 0 ? ", " + skipped + " שורות עם שגיאות דולגו" : ""));
        response.setData(created);
        return response;
    }

    private void fillUser(UserEntity user, BulkImportRowDTO row, Role role, String password) {
        String salt = generateSalt();
        user.setRole(role);
        user.setUsername(row.getUsername());
        user.setUserId(row.getUserId());
        user.setEmail(row.getEmail());
        user.setPhoneNumber(row.getPhone());
        user.setSalt(salt);
        user.setPasswordHash(hashPassword(password, salt));
    }

    /** Parses and checks the whole file against the school and the existing users. */
    BulkImportPreviewDTO validate(String schoolCode, String csv) {
        BulkImportPreviewDTO preview = new BulkImportPreviewDTO();
        List<List<String>> table = parseCsv(csv);
        if (table.isEmpty()) {
            preview.getFileErrors().add("הקובץ ריק");
            return preview;
        }

        Map<String, Integer> columns = mapHeader(table.get(0));
        List<String> missing = REQUIRED_COLUMNS.stream().filter(c -> !columns.containsKey(c)).toList();
        if (!missing.isEmpty()) {
            preview.getFileErrors().add("חסרות עמודות חובה בשורת הכותרת: " + String.join(", ", missing));
            return preview;
        }
        if (table.size() - 1 > MAX_ROWS) {
            preview.getFileErrors().add("ניתן לייבא עד " + MAX_ROWS + " שורות בקובץ אחד");
            return preview;
        }

        Set<String> schoolClasses = new HashSet<>(classRoomRepository.findDistinctClassNamesBySchoolCode(schoolCode));
        Map<String, Integer> seenIds = new HashMap<>();
        Map<String, Integer> seenEmails = new HashMap<>();

        for (int i = 1; i < table.size(); i++) {
            List<String> cells = table.get(i);
            if (cells.stream().allMatch(String::isBlank)) continue;

            BulkImportRowDTO row = new BulkImportRowDTO();
            row.setRowNumber(i + 1);
            row.setRole(parseRole(cell(cells, columns, "role")));
            row.setUsername(cell(cells, columns, "username"));
            row.setUserId(cell(cells, columns, "userId"));
            row.setEmail(cell(cells, columns, "email").toLowerCase());
            row.setPhone(normalizePhone(cell(cells, columns, "phone")));
            row.setClassName(cell(cells, columns, "className"));
            row.setSubjects(splitList(cell(cells, columns, "subjects")));
            row.setChildren(splitList(cell(cells, columns, "children")));

            List<String> errors = row.getErrors();
            if (row.getRole() == null) errors.add("תפקיד לא מוכר (תלמיד / מורה / הורה)");
            if (row.getUsername().isBlank()) errors.add("חסר שם");
            if (!row.getUserId().matches("\\d{5,9}")) errors.add("תעודת זהות חייבת להכיל 5 עד 9 ספרות");
            if (!isValidEmail(row.getEmail())) errors.add("אימייל לא תקין");
            if (!isValidPhoneNumber(row.getPhone())) errors.add("מספר טלפון לא תקין (10 ספרות שמתחילות ב-05)");

            if (!row.getUserId().isBlank()) {
                Integer first = seenIds.putIfAbsent(row.getUserId(), row.getRowNumber());
                if (first != null) errors.add("תעודת זהות כפולה בקובץ (גם בשורה " + first + ")");
                else if (userRepository.findUserByUserId(row.getUserId()) != null) errors.add("תעודת זהות כבר קיימת במערכת");
            }
            if (!row.getEmail().isBlank()) {
                Integer first = seenEmails.putIfAbsent(row.getEmail(), row.getRowNumber());
                if (first != null) errors.add("אימייל כפול בקובץ (גם בשורה " + first + ")");
                else if (userRepository.existsByEmail(row.getEmail())) errors.add("אימייל כבר קיים במערכת");
            }
            if (!row.getPhone().isBlank() && userRepository.existsByPhoneNumber(row.getPhone())) {
                row.getWarnings().add("מספר הטלפון כבר רשום למשתמש אחר");
            }

            if (Role.STUDENT.name().equals(row.getRole())) {
                if (row.getClassName().isBlank()) errors.add("לתלמיד חובה לציין כיתה");
                else if (!schoolClasses.contains(row.getClassName())) errors.add("הכיתה " + row.getClassName() + " לא קיימת בבית הספר");
                if (!row.getChildren().isEmpty()) row.getWarnings().add("עמודת ילדים מתעלמת עבור תלמיד");
            }
            if (Role.TEACHER.name().equals(row.getRole())) {
                for (String className : splitList(row.getClassName())) {
                    if (!schoolClasses.contains(className)) errors.add("הכיתה " + className + " לא קיימת בבית הספר");
                }
                if (row.getSubjects().isEmpty()) row.getWarnings().add("לא צוינו מקצועות למורה");
            }
            preview.getRows().add(row);
        }

        // Parents last: a child may be an existing student of the school or a valid student row in this file.
        Set<String> fileStudents = new HashSet<>();
        for (BulkImportRowDTO row : preview.getRows()) {
            if (Role.STUDENT.name().equals(row.getRole()) && row.isValid()) fileStudents.add(row.getUserId());
        }
        for (BulkImportRowDTO row : preview.getRows()) {
            if (!Role.PARENT.name().equals(row.getRole())) continue;
            if (row.getChildren().isEmpty()) {
                row.getErrors().add("להורה חובה לציין לפחות ילד אחד (ת.ז מופרדות ב-;)");
            }
            for (String childId : row.getChildren()) {
                if (fileStudents.contains(childId)) continue;
                StudentEntity child = studentRepository.findStudentByStudentId(childId);
                if (child == null || child.getSchoolName() == null || !schoolCode.equals(child.getSchoolName().getSchoolCode())) {
                    row.getErrors().add("הילד " + childId + " אינו תלמיד בבית הספר");
                }
            }
        }

        preview.setValidCount((int) preview.getRows().stream().filter(BulkImportRowDTO::isValid).count());
        preview.setErrorCount(preview.getRows().size() - preview.getValidCount());
        if (preview.getRows().isEmpty()) {
            preview.getFileErrors().add("אין שורות נתונים מתחת לשורת הכותרת");
        }
        return preview;
    }

    static String parseRole(String value) {
        String v = value == null ? "" : value.trim().toUpperCase();
        return switch (v) {
            case "STUDENT", "תלמיד", "תלמידה" -> Role.STUDENT.name();
            case "TEACHER", "מורה" -> Role.TEACHER.name();
            case "PARENT", "הורה" -> Role.PARENT.name();
            default -> null;
        };
    }

    /** Strips separators and restores the leading zero spreadsheets drop ("501234567" → "0501234567"). */
    static String normalizePhone(String value) {
        String digits = value == null ? "" : value.replaceAll("[\\s\\-()]", "");
        if (digits.startsWith("+972")) digits = "0" + digits.substring(4);
        if (digits.length() == 9 && digits.startsWith("5")) digits = "0" + digits;
        return digits;
    }

    static List<String> splitList(String value) {
        if (value == null || value.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.stream(value.split("[;|]"))
                .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList());
    }

    static String generateInitialPassword() {
        while (true) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                sb.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
            }
            String password = sb.toString();
            if (password.matches(".*[A-Z].*") && password.matches(".*[a-z].*") && password.matches(".*\\d.*")) {
                return password;
            }
        }
    }

    private static Map<String, Integer> mapHeader(List<String> header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String name = header.get(i).trim().toLowerCase();
            for (Map.Entry<String, List<String>> alias : HEADER_ALIASES.entrySet()) {
                if (alias.getValue().contains(name)) {
                    columns.putIfAbsent(alias.getKey(), i);
                }
            }
        }
        return columns;
    }

    private static String cell(List<String> cells, Map<String, Integer> columns, String column) {
        Integer index = columns.get(column);
        if (index == null || index >= cells.size()) return "";
        return cells.get(index).trim();
    }

    /** RFC 4180 style: comma separated, double quotes around fields, "" for a quote, CRLF or LF rows. */
    static List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        if (csv == null) return rows;
        String text = csv.startsWith("﻿") ? csv.substring(1) : csv;

        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(ch);
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        rows.removeIf(r -> r.stream().allMatch(String::isBlank));
        return rows;
    }
}
