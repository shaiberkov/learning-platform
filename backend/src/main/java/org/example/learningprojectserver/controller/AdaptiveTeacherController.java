package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.AdaptiveDTOs.HintTemplateRequest;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.HintService;
import org.example.learningprojectserver.service.MasteryService;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.Adaptive.*;

/**
 * Teacher side of adaptive learning. Like every route group, open to all callers (see
 * SecurityConfig); the services check that the teacher id exists and owns the class, but the
 * id itself is taken from the request, not the JWT.
 */
@RestController
@RequestMapping(TEACHER_ADAPTIVE_BASE_PATH)
@RequiredArgsConstructor
public class AdaptiveTeacherController {

    private final MasteryService masteryService;
    private final HintService hintService;

    @GetMapping(TEACHER_CLASSES)
    public BasicResponse getTeacherClasses(@RequestParam String teacherId) {
        return masteryService.getTeacherClasses(teacherId);
    }

    @GetMapping(CLASS_SKILL_MAP)
    public BasicResponse getClassSkillMap(@RequestParam String teacherId, @RequestParam Long classRoomId) {
        return masteryService.getClassSkillMap(teacherId, classRoomId);
    }

    @GetMapping(HINT_CATALOG)
    public BasicResponse getHintCatalog() {
        return hintService.getTemplateCatalog();
    }

    @GetMapping(HINT_TEMPLATE)
    public BasicResponse getHintTemplate(@RequestParam String subject, @RequestParam String subTopic) {
        return hintService.getTemplate(subject, subTopic);
    }

    @PostMapping(HINT_TEMPLATE)
    public BasicResponse saveHintTemplate(@RequestParam String teacherId, @RequestBody HintTemplateRequest request) {
        return hintService.saveTemplate(teacherId, request);
    }
}
