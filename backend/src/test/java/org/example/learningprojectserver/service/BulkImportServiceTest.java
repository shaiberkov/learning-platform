package org.example.learningprojectserver.service;

import org.example.learningprojectserver.dto.BulkImportPreviewDTO;
import org.example.learningprojectserver.dto.BulkImportRowDTO;
import org.example.learningprojectserver.entities.SchoolEntity;
import org.example.learningprojectserver.entities.StudentEntity;
import org.example.learningprojectserver.repository.ClassRoomRepository;
import org.example.learningprojectserver.repository.SchoolRepository;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkImportServiceTest {

    @Mock private SchoolRepository schoolRepository;
    @Mock private UserRepository userRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ClassRoomRepository classRoomRepository;
    @Mock private SchoolCacheEvictor cacheEvictor;
    @InjectMocks private BulkImportService service;

    private static final String HEADER = "role,username,userId,email,phone,className,subjects,children\n";

    private void schoolWithClasses() {
        when(classRoomRepository.findDistinctClassNamesBySchoolCode("S1")).thenReturn(List.of("ז1", "ז2"));
        lenient().when(userRepository.findUserByUserId(anyString())).thenReturn(null);
        lenient().when(userRepository.existsByEmail(anyString())).thenReturn(false);
        lenient().when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
    }

    @Test
    void validRowsOfEveryRolePass() {
        schoolWithClasses();
        String csv = HEADER
                + "תלמיד,דנה כהן,111111111,dana@x.com,0501111111,ז1,,\n"
                + "מורה,רון לוי,222222222,ron@x.com,0502222222,ז1;ז2,מתמטיקה;פיזיקה,\n"
                + "הורה,מיכל כהן,333333333,michal@x.com,0503333333,,,111111111\n";

        BulkImportPreviewDTO preview = service.validate("S1", csv);

        assertTrue(preview.getFileErrors().isEmpty());
        assertEquals(3, preview.getValidCount());
        assertEquals(List.of("STUDENT", "TEACHER", "PARENT"), preview.getRows().stream().map(BulkImportRowDTO::getRole).toList());
        assertEquals(List.of("מתמטיקה", "פיזיקה"), preview.getRows().get(1).getSubjects());
    }

    @Test
    void reportsErrorsPerRow() {
        schoolWithClasses();
        when(userRepository.findUserByUserId("444444444")).thenReturn(new StudentEntity());
        when(studentRepository.findStudentByStudentId("999999999")).thenReturn(null);
        String csv = HEADER
                + "תלמיד,א,444444444,a@x.com,0504444444,ז1,,\n"        // id exists
                + "תלמיד,ב,555555555,not-an-email,123,ט9,,\n"          // email, phone, class
                + "מנהל,ג,666666666,c@x.com,0506666666,,,\n"           // role
                + "תלמיד,ד,777777777,a@x.com,0507777777,,,\n"          // duplicate email, missing class
                + "הורה,ה,888888888,e@x.com,0508888888,,,999999999\n"; // unknown child

        List<BulkImportRowDTO> rows = service.validate("S1", csv).getRows();

        assertTrue(rows.get(0).getErrors().contains("תעודת זהות כבר קיימת במערכת"));
        assertEquals(3, rows.get(1).getErrors().size());
        assertEquals(3, rows.get(1).getRowNumber()); // header is row 1
        assertNull(rows.get(2).getRole());
        assertTrue(rows.get(3).getErrors().stream().anyMatch(e -> e.startsWith("אימייל כפול בקובץ")));
        assertTrue(rows.get(3).getErrors().contains("לתלמיד חובה לציין כיתה"));
        assertTrue(rows.get(4).getErrors().contains("הילד 999999999 אינו תלמיד בבית הספר"));
        assertTrue(rows.stream().noneMatch(BulkImportRowDTO::isValid));
    }

    @Test
    void parentMayReferenceAnExistingStudentOfTheSchool() {
        schoolWithClasses();
        SchoolEntity school = new SchoolEntity();
        school.setSchoolCode("S1");
        StudentEntity child = new StudentEntity();
        child.setSchoolName(school);
        when(studentRepository.findStudentByStudentId("121212121")).thenReturn(child);

        BulkImportPreviewDTO preview = service.validate("S1",
                HEADER + "הורה,ה,888888888,e@x.com,0508888888,,,121212121\n");

        assertEquals(1, preview.getValidCount());
    }

    @Test
    void missingHeaderColumnsFailTheFile() {
        BulkImportPreviewDTO preview = service.validate("S1", "username,email\nא,a@x.com\n");
        assertFalse(preview.getFileErrors().isEmpty());
        assertTrue(preview.getRows().isEmpty());
    }
}
