package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.example.learningprojectserver.constants.ControllerConstants.Search.*;

@RestController
@RequestMapping(SEARCH_BASE_PATH)
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping(GLOBAL)
    public BasicResponse search(@RequestParam String userId, @RequestParam String q) {
        return searchService.search(userId, q);
    }

    @GetMapping(STUDENTS_PAGE)
    public BasicResponse studentsPage(@RequestParam String userId,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size) {
        return searchService.studentsPage(userId, q, page, size);
    }
}
