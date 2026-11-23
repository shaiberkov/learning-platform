package org.example.learningprojectserver.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.ParentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.example.learningprojectserver.constants.ControllerConstants.Parent.*;

@RestController
@RequestMapping(PARENT_BASE_PATH)
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @PreAuthorize("@authz.isSelf(#parentId)")
    @GetMapping(GET_CHILDREN)
    public BasicResponse getChildren(@RequestParam String parentId) {
        return parentService.getChildren(parentId);
    }

    @PreAuthorize("@authz.isSelf(#parentId)")
    @GetMapping(CHILD_OVERVIEW)
    public BasicResponse getChildOverview(@RequestParam String parentId, @RequestParam String childId) {
        return parentService.getChildOverview(parentId, childId);
    }
}
