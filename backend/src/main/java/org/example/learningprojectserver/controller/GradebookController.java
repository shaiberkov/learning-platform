package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.GradebookAssignmentRequest;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.gradebook.GradebookService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

import static org.example.learningprojectserver.constants.ControllerConstants.Gradebook.*;

@RestController
@RequestMapping(GRADEBOOK_BASE_PATH)
@RequiredArgsConstructor
public class GradebookController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final GradebookService gradebookService;

    @GetMapping(GET_GRADEBOOK_CLASSES)
    public BasicResponse getClasses(@RequestParam String teacherId) {
        return gradebookService.getClasses(teacherId);
    }

    @GetMapping(GET_GRADEBOOK)
    public BasicResponse getGradebook(@RequestParam String teacherId, @RequestParam Long classRoomId) {
        return gradebookService.getGradebook(teacherId, classRoomId);
    }

    @PostMapping(ADD_GRADEBOOK_ASSIGNMENT)
    public BasicResponse addAssignment(@RequestParam String teacherId, @RequestBody GradebookAssignmentRequest request) {
        return gradebookService.addAssignment(teacherId, request);
    }

    @DeleteMapping(DELETE_GRADEBOOK_ASSIGNMENT)
    public BasicResponse deleteAssignment(@RequestParam String teacherId, @RequestParam Long assignmentId) {
        return gradebookService.deleteAssignment(teacherId, assignmentId);
    }

    @PostMapping(SET_GRADEBOOK_GRADE)
    public BasicResponse setGrade(@RequestParam String teacherId, @RequestParam Long assignmentId,
                                  @RequestParam String studentId, @RequestParam(required = false) Double score) {
        return gradebookService.setGrade(teacherId, assignmentId, studentId, score);
    }

    @PostMapping(SET_GRADEBOOK_WEIGHTS)
    public BasicResponse setWeights(@RequestParam String teacherId, @RequestParam Long classRoomId,
                                    @RequestBody Map<String, Integer> weights) {
        return gradebookService.setWeights(teacherId, classRoomId, weights);
    }

    @GetMapping(EXPORT_GRADEBOOK)
    public ResponseEntity<?> export(@RequestParam String teacherId, @RequestParam Long classRoomId) throws IOException {
        byte[] file = gradebookService.exportExcel(teacherId, classRoomId);
        if (file == null) {
            return ResponseEntity.ok(new BasicResponse<>(false, "הכיתה לא נמצאה או שאינך מלמד בה"));
        }
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(gradebookService.exportFileName(teacherId, classRoomId)).build().toString())
                .body(file);
    }
}
