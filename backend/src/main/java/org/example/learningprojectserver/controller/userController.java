package org.example.learningprojectserver.controller;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    public RegisterResponse registerUser(@NotBlank(message = "יש להזין שם משתמש") @Size(max = 100, message = "שם המשתמש ארוך מדי") @RequestParam String username,
                                         @NotBlank(message = "יש להזין תעודת זהות") @Size(max = 20, message = "תעודת הזהות ארוכה מדי") @RequestParam String userId,
                                         @NotBlank(message = "יש להזין סיסמה") @Size(max = 100, message = "הסיסמה ארוכה מדי") @RequestParam String password,
                                         @NotBlank(message = "יש להזין אימות סיסמה") @RequestParam String confirmPassword,
                                         @NotBlank(message = "יש להזין כתובת אימייל") @Size(max = 255, message = "כתובת האימייל ארוכה מדי") @RequestParam String email,
                                         @NotBlank(message = "יש להזין מספר טלפון") @Size(max = 20, message = "מספר הטלפון ארוך מדי") @RequestParam String phone) {

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
    public BasicResponse loginUser(@NotBlank(message = "יש להזין תעודת זהות") @Size(max = 20, message = "תעודת הזהות ארוכה מדי") @RequestParam String userId, @NotBlank(message = "יש להזין סיסמה") @RequestParam String password) {
        return userService.loginUser(userId, password);
    }
    @PostMapping(SEND_OTP)
    public BasicResponse sendOtp(@NotBlank(message = "יש להזין תעודת זהות") @Size(max = 20, message = "תעודת הזהות ארוכה מדי") @RequestParam String userId, @RequestParam(required = false) String phoneNumber) {
        return userService.sendOtp(userId, phoneNumber);
    }
    @PostMapping(VERIFY_OTP)
    public LoginResponse verifyOtp(@NotBlank(message = "יש להזין תעודת זהות") @Size(max = 20, message = "תעודת הזהות ארוכה מדי") @RequestParam String userId, @NotBlank(message = "יש להזין את קוד האימות") @RequestParam String otp,
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
    public ResetPasswordResponse forgotPassword(@NotBlank(message = "יש להזין תעודת זהות") @Size(max = 20, message = "תעודת הזהות ארוכה מדי") @RequestParam String userId) {
        return userService.sendPasswordResetOtp(userId);
    }

    @PostMapping(RESET_PASSWORD)
    public ResetPasswordResponse resetPassword(@NotBlank(message = "יש להזין תעודת זהות") @Size(max = 20, message = "תעודת הזהות ארוכה מדי") @RequestParam String userId,
                                       @NotBlank(message = "יש להזין את קוד האימות") @RequestParam String otp,
                                       @NotBlank(message = "יש להזין סיסמה חדשה") @Size(max = 100, message = "הסיסמה ארוכה מדי") @RequestParam String newPassword) {
        return userService.resetPassword(userId, otp, newPassword);
    }




}