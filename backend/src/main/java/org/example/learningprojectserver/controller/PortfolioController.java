package org.example.learningprojectserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.PortfolioDTOs;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.CurrentUser;
import org.example.learningprojectserver.service.portfolio.PortfolioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.Portfolio.*;

/** The signed-in student's own portfolio and its share link. The student always comes from the token. */
@RestController
@RequestMapping(PORTFOLIO_BASE_PATH)
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping(MY_PORTFOLIO)
    public BasicResponse<PortfolioDTOs.Portfolio> mine() {
        return portfolioService.getMine(CurrentUser.userId());
    }

    @PostMapping(ADD_PROJECT)
    public BasicResponse<PortfolioDTOs.Project> addProject(@Valid @RequestBody PortfolioDTOs.ProjectRequest request) {
        return portfolioService.addProject(CurrentUser.userId(), request);
    }

    @PostMapping(DELETE_PROJECT)
    public BasicResponse<Void> deleteProject(@RequestParam Long projectId) {
        return portfolioService.deleteProject(CurrentUser.userId(), projectId);
    }

    @PostMapping(CREATE_SHARE)
    public BasicResponse<PortfolioDTOs.Share> createShare(@RequestParam(required = false) String displayName) {
        return portfolioService.createShare(CurrentUser.userId(), displayName);
    }

    @PostMapping(REVOKE_SHARE)
    public BasicResponse<Void> revokeShare() {
        return portfolioService.revokeShare(CurrentUser.userId());
    }
}
