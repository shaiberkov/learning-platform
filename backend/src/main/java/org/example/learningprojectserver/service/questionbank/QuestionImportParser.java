package org.example.learningprojectserver.service.questionbank;

import org.apache.poi.ss.usermodel.*;
import org.example.learningprojectserver.dto.BankQuestionDTO;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;

/**
 * Reads questions from a CSV or Excel (.xlsx / .xls) file. The first row may be a header with the column
 * names in Hebrew or English; without a recognised header the columns are read in the order
 * question, answer, subject, topic, level, grade, options. Options are separated by ';' or '|'.
 */
public final class QuestionImportParser {

    public static final int MAX_ROWS = 1000;

    static final String[] FIELDS = {"questionText", "answer", "subject", "topic", "level", "grade", "options"};

    private static final Map<String, String> HEADER_ALIASES = new HashMap<>();
    static {
        alias("questionText", "שאלה", "טקסט", "question", "text", "questiontext");
        alias("answer", "תשובה", "answer", "תשובה נכונה");
        alias("subject", "מקצוע", "subject");
        alias("topic", "נושא", "topic");
        alias("level", "רמה", "קושי", "רמת קושי", "level", "difficulty");
        alias("grade", "שכבה", "כיתה", "grade");
        alias("options", "אפשרויות", "תשובות אפשריות", "options", "choices");
    }

    private static void alias(String field, String... names) {
        for (String n : names) HEADER_ALIASES.put(n.trim().toLowerCase(Locale.ROOT), field);
    }

    public record Result(List<BankQuestionDTO> questions, List<String> errors) {}

    private QuestionImportParser() {}

    public static Result parse(byte[] content, String fileName) throws IOException {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        List<List<String>> rows = (name.endsWith(".xlsx") || name.endsWith(".xls"))
                ? readExcel(content)
                : readCsv(decode(content));
        return toQuestions(rows);
    }

    static Result toQuestions(List<List<String>> rows) {
        List<BankQuestionDTO> questions = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        if (rows.isEmpty()) {
            errors.add("הקובץ ריק");
            return new Result(questions, errors);
        }

        int[] columnOf = headerColumns(rows.get(0));
        int firstDataRow = 1;
        if (columnOf == null) {
            columnOf = new int[]{0, 1, 2, 3, 4, 5, 6};
            firstDataRow = 0;
        }

        for (int r = firstDataRow; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (row.stream().allMatch(c -> c == null || c.isBlank())) continue;
            if (questions.size() >= MAX_ROWS) {
                errors.add("ניתן לייבא עד " + MAX_ROWS + " שאלות בקובץ אחד; השאר לא יובאו");
                break;
            }
            String text = cell(row, columnOf[0]);
            String answer = cell(row, columnOf[1]);
            if (text.isEmpty() || answer.isEmpty()) {
                errors.add("שורה " + (r + 1) + ": חסרה שאלה או תשובה");
                continue;
            }
            BankQuestionDTO q = new BankQuestionDTO();
            q.setQuestionText(text);
            q.setAnswer(answer);
            q.setSubject(blankToNull(cell(row, columnOf[2])));
            q.setTopic(blankToNull(cell(row, columnOf[3])));
            q.setLevel(blankToNull(cell(row, columnOf[4])));
            q.setGrade(blankToNull(cell(row, columnOf[5])));
            String options = cell(row, columnOf[6]);
            if (!options.isEmpty()) {
                q.setOptions(Arrays.stream(options.split("[;|]"))
                        .map(String::trim).filter(o -> !o.isEmpty()).toList());
            }
            questions.add(q);
        }
        return new Result(questions, errors);
    }

    /** Column index per field, or null when the row is not a header (no question/answer column named). */
    private static int[] headerColumns(List<String> header) {
        int[] columnOf = new int[FIELDS.length];
        Arrays.fill(columnOf, -1);
        for (int c = 0; c < header.size(); c++) {
            String raw = header.get(c) == null ? "" : header.get(c).trim().toLowerCase(Locale.ROOT);
            String field = HEADER_ALIASES.get(raw);
            if (field == null) continue;
            int idx = Arrays.asList(FIELDS).indexOf(field);
            if (columnOf[idx] == -1) columnOf[idx] = c;
        }
        return (columnOf[0] >= 0 && columnOf[1] >= 0) ? columnOf : null;
    }

    private static String cell(List<String> row, int index) {
        if (index < 0 || index >= row.size() || row.get(index) == null) return "";
        return row.get(index).trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    /** UTF-8 (with or without BOM); falls back to Windows-1255, which Hebrew Excel uses for CSV. */
    static String decode(byte[] content) {
        boolean bom = content.length >= 3
                && (content[0] & 0xFF) == 0xEF && (content[1] & 0xFF) == 0xBB && (content[2] & 0xFF) == 0xBF;
        int offset = bom ? 3 : 0;
        ByteBuffer buffer = ByteBuffer.wrap(content, offset, content.length - offset);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(buffer).toString();
        } catch (CharacterCodingException e) {
            return new String(content, offset, content.length - offset, Charset.forName("windows-1255"));
        }
    }

    /** RFC 4180 CSV: quoted fields, doubled quotes, newlines inside quotes. Delimiter is ',', ';' or tab. */
    static List<List<String>> readCsv(String text) {
        int firstLineEnd = text.indexOf('\n');
        String firstLine = firstLineEnd < 0 ? text : text.substring(0, firstLineEnd);
        char delimiter = ',';
        long best = firstLine.chars().filter(ch -> ch == ',').count();
        for (char candidate : new char[]{';', '\t'}) {
            long count = firstLine.chars().filter(ch -> ch == candidate).count();
            if (count > best) {
                best = count;
                delimiter = candidate;
            }
        }

        List<List<String>> rows = new ArrayList<>();
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
            } else if (ch == delimiter) {
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
        return rows;
    }

    private static List<List<String>> readExcel(byte[] content) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (Row r : sheet) {
                if (rows.size() > MAX_ROWS + 1) break;
                List<String> row = new ArrayList<>();
                for (int c = 0; c < Math.max(r.getLastCellNum(), 0); c++) {
                    Cell cell = r.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    row.add(cell == null ? "" : formatter.formatCellValue(cell, evaluator));
                }
                rows.add(row);
            }
        }
        return rows;
    }
}
