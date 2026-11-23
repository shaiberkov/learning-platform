package org.example.learningprojectserver.service;

import org.example.learningprojectserver.entities.*;
import org.example.learningprojectserver.repository.*;
import org.example.learningprojectserver.response.BasicResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** closeYearAndPromote with mocked repositories: moves, refusals and cache evictions. */
class SchoolYearCloseTest {

    private SchoolRepository schoolRepository;
    private SchoolYearRepository schoolYearRepository;
    private ArchivedStudentRecordRepository archiveRepository;
    private SchoolCacheEvictor cacheEvictor;
    private SchoolYearService service;
    private SchoolEntity school;
    private SchoolYearEntity year;

    @BeforeEach
    void setUp() {
        schoolRepository = mock(SchoolRepository.class);
        schoolYearRepository = mock(SchoolYearRepository.class);
        archiveRepository = mock(ArchivedStudentRecordRepository.class);
        SchoolAnalyticsRepository analyticsRepository = mock(SchoolAnalyticsRepository.class);
        cacheEvictor = mock(SchoolCacheEvictor.class);
        service = new SchoolYearService(schoolRepository, schoolYearRepository, archiveRepository,
                analyticsRepository, cacheEvictor);

        school = new SchoolEntity();
        school.setSchoolCode("S1");
        year = new SchoolYearEntity();
        year.setId(1L);
        year.setName("תשפ\"ו");
        when(schoolRepository.findBySchoolCode("S1")).thenReturn(school);
        when(schoolYearRepository.findByIdAndSchoolCode(1L, "S1")).thenReturn(year);
        when(analyticsRepository.findGradeRecordsBySchoolCode("S1")).thenReturn(List.of());
    }

    private SchoolGradeEntity grade(String name, String... classNames) {
        SchoolGradeEntity grade = new SchoolGradeEntity();
        grade.setGradeName(name);
        for (String className : classNames) {
            ClassRoomEntity c = new ClassRoomEntity();
            c.setName(className);
            c.setGrade(grade);
            grade.getClasses().add(c);
        }
        school.getSchoolGrades().add(grade);
        return grade;
    }

    private StudentEntity student(String userId, ClassRoomEntity classRoom) {
        StudentEntity s = new StudentEntity();
        s.setUserId(userId);
        s.setUsername("תלמיד " + userId);
        s.setClassRoom(classRoom);
        classRoom.getStudents().add(s);
        return s;
    }

    @Test
    void refusesWhenTheTargetGradeHasNoClassesAndChangesNothing() {
        SchoolGradeEntity seventh = grade("ז", "ז1");
        grade("ח"); // no classes
        grade("ט", "ט1");
        StudentEntity s = student("111", seventh.getClasses().get(0));

        BasicResponse response = service.closeYearAndPromote("S1", 1L, null, null);

        assertFalse(response.isSuccess());
        assertTrue(response.getErrorCode().contains("ח"));
        assertSame(seventh.getClasses().get(0), s.getClassRoom());
        assertFalse(year.isClosed());
        verify(archiveRepository, never()).save(any());
    }

    @Test
    void promotesAndEvictsTheMovedStudentsCaches() {
        SchoolGradeEntity seventh = grade("ז", "ז1");
        SchoolGradeEntity eighth = grade("ח", "ח1");
        StudentEntity s = student("111", seventh.getClasses().get(0));

        BasicResponse response = service.closeYearAndPromote("S1", 1L, null, null);

        assertTrue(response.isSuccess());
        assertSame(eighth.getClasses().get(0), s.getClassRoom());
        assertTrue(year.isClosed());
        verify(cacheEvictor).studentSchedule("111");
        verify(cacheEvictor).studentDashboard("111");
    }
}
