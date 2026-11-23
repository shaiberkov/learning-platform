package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.PortfolioDTOs;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.portfolio.PortfolioService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.example.learningprojectserver.constants.ControllerConstants.Portfolio.PORTFOLIO_PUBLIC_BASE_PATH;
import static org.example.learningprojectserver.constants.ControllerConstants.Portfolio.PUBLIC_PORTFOLIO;

/**
 * The read-only page behind a portfolio share link. permitAll in SecurityConfig for GET of exactly this
 * path: the secret token is the only credential, and the answer holds only the public shape.
 */
@RestController
@RequestMapping(PORTFOLIO_PUBLIC_BASE_PATH)
@RequiredArgsConstructor
public class PortfolioPublicController {

    private final PortfolioService portfolioService;

    @GetMapping(PUBLIC_PORTFOLIO)
    public ResponseEntity<BasicResponse<PortfolioDTOs.PublicPortfolio>> view(@PathVariable String token) {
        PortfolioDTOs.PublicPortfolio portfolio = portfolioService.getPublic(token);
        if (portfolio == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new BasicResponse<>(false, "הקישור לא קיים או שהשיתוף בוטל"));
        }
        BasicResponse<PortfolioDTOs.PublicPortfolio> body = new BasicResponse<>(true, "");
        body.setData(portfolio);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex")
                .body(body);
    }
}
