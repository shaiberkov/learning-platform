package org.example.learningprojectserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.response.TokenValidationResponse;
import org.example.learningprojectserver.security.AuthCookies;
import org.example.learningprojectserver.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.Session.*;

@RestController
@RequestMapping(SESSION_BASE_PATH)
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;


    @GetMapping(VALIDATE_TOKEN)
    public TokenValidationResponse validateToken(HttpServletRequest request) {
        String token = AuthCookies.resolveAccessToken(request);
        return sessionService.validateToken(token);
    }



}