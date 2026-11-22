package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.LiveQuestionRequest;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.livequiz.LiveQuizService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.example.learningprojectserver.constants.ControllerConstants.LiveQuiz.*;

@RestController
@RequestMapping(LIVE_QUIZ_BASE_PATH)
@RequiredArgsConstructor
public class LiveQuizController {

    private final LiveQuizService liveQuizService;

    @PostMapping(START_LIVE_QUIZ)
    public BasicResponse start(@RequestParam String teacherId, @RequestParam Long classRoomId) {
        return liveQuizService.start(teacherId, classRoomId);
    }

    @PostMapping(PUSH_LIVE_QUESTION)
    public BasicResponse pushQuestion(@RequestParam String teacherId, @RequestParam String code,
                                      @RequestBody LiveQuestionRequest question) {
        return liveQuizService.pushQuestion(teacherId, code, question);
    }

    @PostMapping(CLOSE_LIVE_QUESTION)
    public BasicResponse closeQuestion(@RequestParam String teacherId, @RequestParam String code) {
        return liveQuizService.closeQuestion(teacherId, code);
    }

    @PostMapping(END_LIVE_QUIZ)
    public BasicResponse end(@RequestParam String teacherId, @RequestParam String code) {
        return liveQuizService.end(teacherId, code);
    }

    @GetMapping(GET_LIVE_QUIZ_RESULTS)
    public BasicResponse getResults(@RequestParam String teacherId, @RequestParam(required = false) String code) {
        return liveQuizService.getResults(teacherId, code);
    }

    @GetMapping(GET_CURRENT_LIVE_QUIZ)
    public BasicResponse getCurrent(@RequestParam String studentId) {
        return liveQuizService.getCurrent(studentId);
    }

    /** Body: {"answer": "..."} */
    @PostMapping(ANSWER_LIVE_QUESTION)
    public BasicResponse answer(@RequestParam String studentId, @RequestParam String code,
                                @RequestBody Map<String, String> body) {
        return liveQuizService.answer(studentId, code, body == null ? null : body.get("answer"));
    }
}
