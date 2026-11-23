package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.DiagnosisDTOs.FeynmanExplanationDTO;
import org.example.learningprojectserver.dto.DiagnosisDTOs.FeynmanPromptDTO;
import org.example.learningprojectserver.dto.DiagnosisDTOs.FeynmanSubmitRequest;
import org.example.learningprojectserver.dto.DiagnosisDTOs.MistakeDiagnosisDTO;
import org.example.learningprojectserver.dto.DiagnosisDTOs.MistakeItemDTO;
import org.example.learningprojectserver.dto.DiagnosisDTOs.SelfCheckResultDTO;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.diagnosis.FeynmanService;
import org.example.learningprojectserver.service.diagnosis.MistakeDiagnosisService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.example.learningprojectserver.constants.ControllerConstants.Diagnosis.*;

/**
 * [s06] Student side of "explain it to me" (Feynman) and the mistake-type diagnosis. Under /student
 * (STUDENT and SYSTEM_ADMIN in SecurityConfig). The student is always the authenticated user, never a parameter.
 */
@RestController
@RequestMapping(STUDENT_DIAGNOSIS_BASE_PATH)
@RequiredArgsConstructor
public class StudentDiagnosisController {

    private final FeynmanService feynmanService;
    private final MistakeDiagnosisService mistakeDiagnosisService;

    @GetMapping(FEYNMAN_PROMPTS)
    public BasicResponse<List<FeynmanPromptDTO>> feynmanPrompts() {
        return feynmanService.prompts();
    }

    @GetMapping(FEYNMAN_EXPLANATIONS)
    public BasicResponse<List<FeynmanExplanationDTO>> feynmanHistory() {
        return feynmanService.history(CurrentUser.userId());
    }

    @PostMapping(FEYNMAN_EXPLANATIONS)
    public BasicResponse<FeynmanExplanationDTO> submitExplanation(@RequestBody FeynmanSubmitRequest request) {
        return feynmanService.submit(CurrentUser.userId(), request);
    }

    @GetMapping(MISTAKE_DIAGNOSIS)
    public BasicResponse<MistakeDiagnosisDTO> mistakeDiagnosis() {
        return mistakeDiagnosisService.diagnosis(CurrentUser.userId());
    }

    @PostMapping(MISTAKE_SELF_CHECK)
    public BasicResponse<SelfCheckResultDTO> selfCheck(@PathVariable Long itemId, @RequestParam String answer) {
        return mistakeDiagnosisService.selfCheck(CurrentUser.userId(), itemId, answer);
    }

    @PostMapping(MISTAKE_AI_ANALYSIS)
    public BasicResponse<MistakeItemDTO> analyzeWithAi(@PathVariable Long itemId) {
        return mistakeDiagnosisService.analyzeWithAi(CurrentUser.userId(), itemId);
    }
}
