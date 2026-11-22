package org.example.learningprojectserver.controller;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.UserDto;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.response.LoginResponse;
import org.example.learningprojectserver.response.RegisterResponse;
import org.example.learningprojectserver.response.ResetPasswordResponse;
import org.example.learningprojectserver.security.AuthCookies;
import org.example.learningprojectserver.service.AuthTokenService;
import org.example.learningprojectserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static org.example.learningprojectserver.constants.ControllerConstants.User.*;

@RestController
@RequestMapping(USER_BASE_PATH)
@RequiredArgsConstructor
public class userController {

private final UserService userService;
private final AuthTokenService authTokenService;
private final AuthCookies authCookies;


    @PostMapping(ADD_USER)
    public RegisterResponse registerUser(@RequestParam String username,
                                         @RequestParam String userId,
                                         @RequestParam String password,
                                         @RequestParam String confirmPassword,
                                         @RequestParam String email,
                                         @RequestParam String phone) {

        return userService.createUser(
                username,
                userId,
                password,
                confirmPassword,
                email,
                phone
        );
    }
    @PostMapping(LOGIN)
    public BasicResponse loginUser(@RequestParam String userId, @RequestParam String password) {
        return userService.loginUser(userId, password);
    }
    @PostMapping(SEND_OTP)
    public BasicResponse sendOtp(@RequestParam String userId, @RequestParam String phoneNumber) {
        return userService.sendOtp(userId, phoneNumber);
    }
    @PostMapping(VERIFY_OTP)
    public LoginResponse verifyOtp(@RequestParam String userId, @RequestParam String otp,
                                   HttpServletResponse response) {
        LoginResponse loginResponse = userService.verifyOtp(userId, otp);
        if (loginResponse.isSuccess()) {
            authCookies.write(response, loginResponse.getTokens());
        }
        return loginResponse;
    }

    @PostMapping(REFRESH_TOKEN)
    public BasicResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        AuthTokenService.RotationResult result = authTokenService.rotate(AuthCookies.readRefreshToken(request));
        switch (result.status()) {
            case ROTATED -> authCookies.write(response, result.tokens());
            case ALREADY_ROTATED -> { } // a parallel request already set the new cookies
            default -> {
                authCookies.clear(response);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return new BasicResponse(false, "פג תוקף ההתחברות, יש להתחבר מחדש");
            }
        }
        return new BasicResponse(true, "");
    }

    @PostMapping(LOGOUT)
    public BasicResponse logout(HttpServletRequest request, HttpServletResponse response) {
        authTokenService.revoke(AuthCookies.readRefreshToken(request));
        authCookies.clear(response);
        return new BasicResponse(true, "");
    }

    @PostMapping(FORGOT_PASSWORD)
    public ResetPasswordResponse forgotPassword(@RequestParam String userId) {
        return userService.sendPasswordResetOtp(userId);
    }

    @PostMapping(RESET_PASSWORD)
    public ResetPasswordResponse resetPassword(@RequestParam String userId,
                                       @RequestParam String otp,
                                       @RequestParam String newPassword) {
        return userService.resetPassword(userId, otp, newPassword);
    }




}