package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.BankQuestionDTO;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.questionbank.QuestionBankService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.example.learningprojectserver.constants.ControllerConstants.QuestionBank.*;

@RestController
@RequestMapping(QUESTION_BANK_BASE_PATH)
@RequiredArgsConstructor
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    @PostMapping(ADD_BANK_QUESTION)
    public BasicResponse addQuestion(@RequestParam String teacherId, @RequestBody BankQuestionDTO question) {
        return questionBankService.addQuestion(teacherId, question);
    }

    @PutMapping(UPDATE_BANK_QUESTION)
    public BasicResponse updateQuestion(@RequestParam String teacherId, @RequestParam Long questionId,
                                        @RequestBody BankQuestionDTO question) {
        return questionBankService.updateQuestion(teacherId, questionId, question);
    }

    @DeleteMapping(DELETE_BANK_QUESTION)
    public BasicResponse deleteQuestion(@RequestParam String teacherId, @RequestParam Long questionId) {
        return questionBankService.deleteQuestion(teacherId, questionId);
    }

    @PostMapping(SET_BANK_QUESTION_SHARED)
    public BasicResponse setShared(@RequestParam String teacherId, @RequestParam Long questionId,
                                   @RequestParam boolean shared) {
        return questionBankService.setShared(teacherId, questionId, shared);
    }

    @GetMapping(SEARCH_BANK_QUESTIONS)
    public BasicResponse search(@RequestParam String teacherId,
                                @RequestParam(required = false) String scope,
                                @RequestParam(required = false) String text,
                                @RequestParam(required = false) String subject,
                                @RequestParam(required = false) String topic,
                                @RequestParam(required = false) String level,
                                @RequestParam(required = false) String grade) {
        return questionBankService.search(teacherId, scope, text, subject, topic, level, grade);
    }

    @GetMapping(GET_BANK_TAGS)
    public BasicResponse getTags(@RequestParam String teacherId) {
        return questionBankService.getTags(teacherId);
    }

    @PostMapping(IMPORT_BANK_QUESTIONS)
    public BasicResponse importQuestions(@RequestParam String teacherId,
                                         @RequestParam(defaultValue = "false") boolean shared,
                                         @RequestParam("file") MultipartFile file) {
        return questionBankService.importQuestions(teacherId, file, shared);
    }
}
