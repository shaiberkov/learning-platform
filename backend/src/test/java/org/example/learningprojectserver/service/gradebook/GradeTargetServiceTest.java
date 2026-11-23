package org.example.learningprojectserver.service.gradebook;

import org.example.learningprojectserver.dto.GradebookDTO;
import org.example.learningprojectserver.dto.examsim.GradeTargetDTOs.*;
import org.example.learningprojectserver.entities.ClassRoomEntity;
import org.example.learningprojectserver.entities.StudentEntity;
import org.example.learningprojectserver.entities.TeacherEntity;
import org.example.learningprojectserver.enums.GradeCategory;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GradeTargetServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock GradebookService gradebookService;
    @InjectMocks GradeTargetService service;

    private static final Map<GradeCategory, Integer> EXAM60_HW40 = Map.of(GradeCategory.EXAM, 60, GradeCategory.HOMEWORK, 40);

    @Test
    void solvesTheUniformScoreNeeded() {
        // Exams: 80 graded, one remaining. Homework: 90. F(x) = 0.6 * (80 + x) / 2 + 0.4 * 90.
        List<GradeTargetCalculator.Column> cols = List.of(
                new GradeTargetCalculator.Column(GradeCategory.EXAM, 80.0),
                new GradeTargetCalculator.Column(GradeCategory.EXAM, null),
                new GradeTargetCalculator.Column(GradeCategory.HOMEWORK, 90.0));
        GradeTargetCalculator.Plan p = GradeTargetCalculator.plan(cols, EXAM60_HW40, 90);
        assertEquals(GradeTargetCalculator.Status.POSSIBLE, p.status());
        assertEquals(84.0, p.current());
        assertEquals(100.0, p.requiredPercent()); // 0.3 * (80 + x) + 36 = 90 -> x = 100
        assertEquals(GradeTargetCalculator.Status.IMPOSSIBLE, GradeTargetCalculator.plan(cols, EXAM60_HW40, 95).status());
        assertEquals(GradeTargetCalculator.Status.ALREADY_SECURED, GradeTargetCalculator.plan(cols, EXAM60_HW40, 50).status());
        assertEquals(70.0, GradeTargetCalculator.plan(cols, EXAM60_HW40, 81).requiredPercent());
    }

    @Test
    void reportsNoRemainingAndNoData() {
        List<GradeTargetCalculator.Column> done = List.of(new GradeTargetCalculator.Column(GradeCategory.EXAM, 70.0));
        assertEquals(GradeTargetCalculator.Status.NO_REMAINING_MISSED, GradeTargetCalculator.plan(done, EXAM60_HW40, 90).status());
        assertEquals(GradeTargetCalculator.Status.NO_REMAINING_MET, GradeTargetCalculator.plan(done, EXAM60_HW40, 60).status());
        assertEquals(GradeTargetCalculator.Status.NO_DATA, GradeTargetCalculator.plan(List.of(), EXAM60_HW40, 60).status());
        // A remaining column in a category weighted 0 does not count as remaining.
        List<GradeTargetCalculator.Column> zero = List.of(new GradeTargetCalculator.Column(GradeCategory.EXAM, 70.0),
                new GradeTargetCalculator.Column(GradeCategory.QUIZ, null));
        assertEquals(GradeTargetCalculator.Status.NO_REMAINING_MISSED, GradeTargetCalculator.plan(zero, EXAM60_HW40, 90).status());
    }

    @Test
    void rejectsTargetsOutOfRangeAndStudentsWithoutClass() {
        assertFalse(service.calculate("s1", 120.0).isSuccess());
        assertFalse(service.calculate("s1", null).isSuccess());
        when(studentRepository.findStudentWithClassRoomByUserId("s1")).thenReturn(null);
        BasicResponse<GradeTargetDTO> res = service.calculate("s1", 90.0);
        assertFalse(res.isSuccess());
        assertTrue(res.getErrorCode().contains("כיתה"));
    }

    @Test
    void usesOnlyTheStudentsOwnGradesFromTheTeachersGradebook() {
        ClassRoomEntity classRoom = new ClassRoomEntity();
        classRoom.setId(7L);
        TeacherEntity teacher = new TeacherEntity();
        teacher.setUserId("t1");
        teacher.setUsername("דנה");
        teacher.getTeachingSubjects().add("מתמטיקה");
        classRoom.getTeachers().add(teacher);
        StudentEntity student = new StudentEntity();
        student.setUserId("s1");
        student.setClassRoom(classRoom);
        when(studentRepository.findStudentWithClassRoomByUserId("s1")).thenReturn(student);

        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("EXAM", 60);
        weights.put("HOMEWORK", 40);
        weights.put("QUIZ", 0);
        weights.put("PROJECT", 0);
        Map<String, Map<String, Double>> grades = new HashMap<>();
        grades.put("a-1", Map.of("s1", 40.0, "s2", 50.0)); // exam out of 50 -> 80%
        grades.put("a-2", Map.of("s2", 100.0));              // upcoming exam, graded only for s2
        grades.put("a-3", Map.of("s1", 90.0));               // homework
        List<GradebookDTO.Column> columns = List.of(
                new GradebookDTO.Column("a-1", 1L, null, "מבחן 1", "EXAM", "מבחנים", 50, "2026-10-01", "MANUAL", true),
                new GradebookDTO.Column("a-2", 2L, null, "מבחן 2", "EXAM", "מבחנים", 100, "2026-12-01", "MANUAL", true),
                new GradebookDTO.Column("a-3", 3L, null, "ש\"ב", "HOMEWORK", "שיעורי בית", 100, "2026-10-02", "MANUAL", true),
                new GradebookDTO.Column("t-9", null, 9L, "מבחן ישן", "EXAM", "מבחנים", 100, "01/01/2020", "TEACHER_TEST", false));
        GradebookDTO book = new GradebookDTO(7L, "י1", List.of(), columns, grades, weights, Map.of(), Map.of(), Map.of(), null);
        BasicResponse<GradebookDTO> bookResponse = new BasicResponse<>(true, null);
        bookResponse.setData(book);
        when(gradebookService.getGradebook("t1", 7L)).thenReturn(bookResponse);

        GradeTargetDTO dto = service.calculate("s1", 81.0).getData();
        SubjectPlanDTO plan = dto.subjects().get(0);
        assertEquals("מתמטיקה", plan.title());
        assertEquals("POSSIBLE", plan.status());
        assertEquals(84.0, plan.currentGrade());
        assertEquals(70.0, plan.requiredPercent());
        assertEquals(1, plan.remaining().size()); // the past system test without a result is not "remaining"
        assertEquals("מבחן 2", plan.remaining().get(0).title());
        assertEquals(70.0, plan.remaining().get(0).requiredScore());
        assertTrue(plan.message().contains("70"));
        assertEquals("POSSIBLE", dto.overall().status());
        assertEquals(70.0, dto.overall().requiredPercent());
    }
}
