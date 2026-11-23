package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.DiagnosisDTOs.ClassMistakeSummaryDTO;
import org.example.learningprojectserver.dto.DiagnosisDTOs.FeynmanExplanationDTO;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.diagnosis.FeynmanService;
import org.example.learningprojectserver.service.diagnosis.MistakeDiagnosisService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.learningprojectserver.constants.ControllerConstants.Diagnosis.*;

/**
 * [s06] Teacher view of the mistake types and the "explain it to me" attempts of a class. Under /teacher
 * (TEACHER_ALL). The teacher is the authenticated user; the services answer only for the teacher's own classes.
 */
@RestController
@RequestMapping(TEACHER_DIAGNOSIS_BASE_PATH)
@RequiredArgsConstructor
public class TeacherDiagnosisController {

    private final FeynmanService feynmanService;
    private final MistakeDiagnosisService mistakeDiagnosisService;

    @GetMapping(CLASS_MISTAKE_TYPES)
    public BasicResponse<ClassMistakeSummaryDTO> classMistakeTypes(@RequestParam Long classRoomId) {
        return mistakeDiagnosisService.classSummary(CurrentUser.userId(), classRoomId);
    }

    @GetMapping(CLASS_FEYNMAN)
    public BasicResponse<List<FeynmanExplanationDTO>> classFeynman(@RequestParam Long classRoomId) {
        return feynmanService.classExplanations(CurrentUser.userId(), classRoomId);
    }
}
