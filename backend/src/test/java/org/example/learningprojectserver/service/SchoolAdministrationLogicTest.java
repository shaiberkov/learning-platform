package org.example.learningprojectserver.service;

import org.example.learningprojectserver.dto.*;
import org.example.learningprojectserver.entities.ClassRoomEntity;
import org.example.learningprojectserver.entities.SchoolGradeEntity;
import org.example.learningprojectserver.utils.GradeLevels;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pure logic of the school-administration features: no database, no Spring context. */
class SchoolAdministrationLogicTest {

    // ---- grade levels ----

    @Test
    void gradeLevelsReadHebrewLettersAndDigits() {
        assertEquals(7, GradeLevels.level("ז'"));
        assertEquals(11, GradeLevels.level("י\"א"));
        assertEquals(12, GradeLevels.level("יב"));
        assertEquals(9, GradeLevels.level("9"));
        assertEquals(-1, GradeLevels.level("מחוננים"));
        assertEquals(-1, GradeLevels.level(null));
    }

    // ---- timetable conflicts ----

    private static TimetableLessonDTO lesson(Long id, String teacher, String cls, String room, DayOfWeek day, int startH, int endH) {
        return new TimetableLessonDTO(id, "מתמטיקה", day, LocalTime.of(startH, 0), LocalTime.of(endH, 0),
                cls, teacher, "מורה " + teacher, room);
    }

    @Test
    void detectsTeacherClassAndRoomConflicts() {
        List<TimetableLessonDTO> existing = List.of(
                lesson(1L, "T1", "ז1", "101", DayOfWeek.SUNDAY, 8, 9),
                lesson(2L, "T2", "ז2", "202", DayOfWeek.SUNDAY, 8, 9),
                lesson(3L, "T3", "ח1", "303", DayOfWeek.SUNDAY, 8, 9));

        TimetableLessonDTO draft = lesson(null, "T1", "ז2", "303", DayOfWeek.SUNDAY, 8, 9);
        List<TimetableConflictDTO> conflicts = TimetableService.findConflicts(draft, existing);

        assertEquals(List.of("TEACHER", "CLASS", "ROOM"), conflicts.stream().map(TimetableConflictDTO::getType).toList());
        assertEquals(List.of(1L, 2L, 3L), conflicts.stream().map(TimetableConflictDTO::getConflictingLessonId).toList());
    }

    @Test
    void touchingLessonsOtherDaysAndTheLessonItselfDoNotConflict() {
        List<TimetableLessonDTO> existing = List.of(
                lesson(1L, "T1", "ז1", "101", DayOfWeek.SUNDAY, 8, 9),
                lesson(2L, "T1", "ז1", "101", DayOfWeek.MONDAY, 9, 10));

        assertTrue(TimetableService.findConflicts(lesson(null, "T1", "ז1", "101", DayOfWeek.SUNDAY, 9, 10), existing).isEmpty());
        // moving lesson 2 within its own slot only meets itself
        assertTrue(TimetableService.findConflicts(lesson(2L, "T1", "ז1", "101", DayOfWeek.MONDAY, 9, 10), existing).isEmpty());
    }

    @Test
    void blankRoomsNeverClash() {
        List<TimetableLessonDTO> existing = List.of(lesson(1L, "T1", "ז1", null, DayOfWeek.SUNDAY, 8, 9));
        assertTrue(TimetableService.findConflicts(lesson(null, "T2", "ז2", "", DayOfWeek.SUNDAY, 8, 9), existing).isEmpty());
    }

    @Test
    void rejectsMalformedDrafts() {
        TimetableLessonDTO saturday = lesson(null, "T1", "ז1", null, DayOfWeek.SATURDAY, 8, 9);
        assertNotNull(TimetableService.validateDraft(saturday));
        TimetableLessonDTO backwards = lesson(null, "T1", "ז1", null, DayOfWeek.SUNDAY, 10, 9);
        assertNotNull(TimetableService.validateDraft(backwards));
        assertNull(TimetableService.validateDraft(lesson(null, "T1", "ז1", null, DayOfWeek.SUNDAY, 8, 9)));
    }

    // ---- analytics ----

    @Test
    void buildsAveragesTrendAndClassComparison() {
        List<GradeRecordDTO> records = List.of(
                new GradeRecordDTO("s1", "a", 80, "חשבון", "שברים", "01/09/2026 10:00", "t1", "דנה", "ז1", "ז'"),
                new GradeRecordDTO("s2", "b", 60, "חשבון", "שברים", "15/09/2026 10:00", "t1", "דנה", "ז2", "ז'"),
                new GradeRecordDTO("s3", "c", 90, "גאומטריה", "זוויות", "02/10/2026 10:00", "t2", "רון", "ח1", "ח'"),
                new GradeRecordDTO("s4", "d", 70, "גאומטריה", "זוויות", "bad date", "t2", "רון", "ח1", "ח'"));

        SchoolAnalyticsDTO dto = SchoolAnalyticsService.buildAnalytics(records);

        assertEquals(4, dto.getResultsCount());
        assertEquals(75.0, dto.getOverallAverage());
        assertEquals(List.of("ז'", "ח'"), dto.getByGrade().stream().map(AnalyticsBucketDTO::getLabel).toList());
        assertEquals(70.0, dto.getByGrade().get(0).getAverage());
        assertEquals(List.of("2026-09", "2026-10"), dto.getTrend().stream().map(AnalyticsBucketDTO::getLabel).toList());
        assertEquals(3, dto.getTrend().stream().mapToInt(AnalyticsBucketDTO::getCount).sum());
        assertEquals("ז'", dto.getClassComparison().get(0).getGradeName());
        assertEquals(List.of("ז1", "ז2"), dto.getClassComparison().get(0).getClasses().stream().map(AnalyticsBucketDTO::getLabel).toList());
    }

    @Test
    void emptySchoolHasZeroedAnalytics() {
        SchoolAnalyticsDTO dto = SchoolAnalyticsService.buildAnalytics(List.of());
        assertEquals(0, dto.getResultsCount());
        assertTrue(dto.getByGrade().isEmpty());
    }

    // ---- year promotion ----

    @Test
    void defaultPlanPromotesEachGradeAndGraduatesTheTop() {
        List<GradePromotionDTO> plan = SchoolYearService.defaultPlan(List.of("ט'", "ז'", "ח'", "מחוננים"));
        assertEquals("ז'", plan.get(1).getFromGrade());
        assertEquals("ח'", plan.get(1).getToGrade());
        assertEquals("ט'", plan.get(2).getToGrade());
        assertEquals("", plan.get(3).getToGrade());
        // an unrecognised grade name stays where it is
        GradePromotionDTO unknown = plan.stream().filter(p -> p.getFromGrade().equals("מחוננים")).findFirst().orElseThrow();
        assertEquals("מחוננים", unknown.getToGrade());
    }

    @Test
    void targetClassKeepsTheParallelNumber() {
        SchoolGradeEntity grade8 = new SchoolGradeEntity();
        grade8.setGradeName("ח");
        for (String name : List.of("ח1", "ח2")) {
            ClassRoomEntity c = new ClassRoomEntity();
            c.setName(name);
            c.setGrade(grade8);
            grade8.getClasses().add(c);
        }
        assertEquals("ח2", SchoolYearService.targetClass("ז2", "ז", grade8).getName());
        // no parallel class: first class of the grade
        assertEquals("ח1", SchoolYearService.targetClass("ז3", "ז", grade8).getName());
        assertNull(SchoolYearService.targetClass("ז1", "ז", new SchoolGradeEntity()));
    }

    // ---- bulk import parsing ----

    @Test
    void parsesQuotedCsvWithBomAndCrLf() {
        String csv = "﻿role,username,userId\r\nתלמיד,\"כהן, דנה\",123456789\r\n\r\nמורה,\"אבי \"\"המורה\"\"\",987654321";
        List<List<String>> rows = BulkImportService.parseCsv(csv);
        assertEquals(3, rows.size());
        assertEquals("role", rows.get(0).get(0));
        assertEquals("כהן, דנה", rows.get(1).get(1));
        assertEquals("אבי \"המורה\"", rows.get(2).get(1));
    }

    @Test
    void normalisesRolesPhonesAndLists() {
        assertEquals("STUDENT", BulkImportService.parseRole("תלמיד"));
        assertEquals("PARENT", BulkImportService.parseRole("parent"));
        assertNull(BulkImportService.parseRole("מנהל"));
        assertEquals("0501234567", BulkImportService.normalizePhone("50-123-4567"));
        assertEquals("0501234567", BulkImportService.normalizePhone("+972501234567"));
        assertEquals(List.of("a", "b"), BulkImportService.splitList(" a; b ;a"));
    }

    @Test
    void initialPasswordsAreStrongAndDiffer() {
        String p1 = BulkImportService.generateInitialPassword();
        String p2 = BulkImportService.generateInitialPassword();
        assertEquals(10, p1.length());
        assertTrue(p1.matches("(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10}"));
        assertNotEquals(p1, p2);
    }
}
