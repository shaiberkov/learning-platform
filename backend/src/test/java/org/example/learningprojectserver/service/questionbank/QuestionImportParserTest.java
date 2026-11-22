package org.example.learningprojectserver.service.questionbank;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.example.learningprojectserver.dto.BankQuestionDTO;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionImportParserTest {

    @Test
    void readsCsvWithHebrewHeaderQuotesAndOptions() throws Exception {
        String csv = "﻿שאלה,תשובה,מקצוע,נושא,רמה,שכבה,אפשרויות\r\n"
                + "\"כמה זה 2+2, בערך?\",4,מתמטיקה,חיבור,קל,ג,3;4;5\r\n"
                + "\"הוא אמר \"\"שלום\"\"\",שלום,,,,,\r\n"
                + ",חסרה שאלה,,,,,\r\n";
        QuestionImportParser.Result result = QuestionImportParser.parse(csv.getBytes(StandardCharsets.UTF_8), "q.csv");

        assertEquals(2, result.questions().size());
        BankQuestionDTO first = result.questions().get(0);
        assertEquals("כמה זה 2+2, בערך?", first.getQuestionText());
        assertEquals("4", first.getAnswer());
        assertEquals("מתמטיקה", first.getSubject());
        assertEquals("קל", first.getLevel());
        assertEquals("ג", first.getGrade());
        assertEquals(List.of("3", "4", "5"), first.getOptions());
        assertEquals("הוא אמר \"שלום\"", result.questions().get(1).getQuestionText());
        assertNull(result.questions().get(1).getSubject());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("4"));
    }

    @Test
    void readsCsvWithoutHeaderPositionallyAndSemicolons() throws Exception {
        String csv = "5*5;25;math;mult;hard;5\n";
        QuestionImportParser.Result result = QuestionImportParser.parse(csv.getBytes(StandardCharsets.UTF_8), "q.csv");
        assertEquals(1, result.questions().size());
        assertEquals("25", result.questions().get(0).getAnswer());
        assertEquals("hard", result.questions().get(0).getLevel());
    }

    @Test
    void fallsBackToWindows1255ForHebrewExcelCsv() throws Exception {
        byte[] bytes = "שאלה,תשובה\nמה שלומך,טוב\n".getBytes(Charset.forName("windows-1255"));
        QuestionImportParser.Result result = QuestionImportParser.parse(bytes, "q.csv");
        assertEquals(1, result.questions().size());
        assertEquals("טוב", result.questions().get(0).getAnswer());
    }

    @Test
    void readsXlsx() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("question");
            header.createCell(1).setCellValue("answer");
            header.createCell(2).setCellValue("grade");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("3*4");
            row.createCell(1).setCellValue(12);
            row.createCell(2).setCellValue("ד");
            wb.write(out);
        }
        QuestionImportParser.Result result = QuestionImportParser.parse(out.toByteArray(), "bank.xlsx");
        assertEquals(1, result.questions().size());
        assertEquals("12", result.questions().get(0).getAnswer());
        assertEquals("ד", result.questions().get(0).getGrade());
    }

    @Test
    void validateRequiresAnswerAmongOptions() {
        BankQuestionDTO dto = new BankQuestionDTO();
        dto.setQuestionText("2+2");
        dto.setAnswer("4");
        dto.setOptions(List.of("3", "5"));
        assertNotNull(QuestionBankService.validate(dto));
        dto.setOptions(List.of("3", "4"));
        assertNull(QuestionBankService.validate(dto));
        dto.setAnswer(" ");
        assertNotNull(QuestionBankService.validate(dto));
    }
}
