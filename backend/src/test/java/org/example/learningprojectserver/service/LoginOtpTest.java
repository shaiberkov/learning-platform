package org.example.learningprojectserver.service;

import org.example.learningprojectserver.entities.StudentEntity;
import org.example.learningprojectserver.projection.UserCredentialsProjection;
import org.example.learningprojectserver.repository.StudentRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.response.LoginResponse;
import org.example.learningprojectserver.utils.SmsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.example.learningprojectserver.utils.GeneratorUtils.hashPassword;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** The OTP login step: it needs a fresh password login, uses the stored phone and limits wrong codes. */
class LoginOtpTest {

    private static final String SALT = "salt";
    private static final String STORED_PHONE = "0501111111";

    private UserRepository userRepository;
    private SmsSender smsSender;
    private UserService userService;
    private String userId;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        smsSender = mock(SmsSender.class);
        userService = new UserService(userRepository, mock(ActiveUserService.class), mock(AuthTokenService.class),
                smsSender, mock(StudentRepository.class));
        userId = UUID.randomUUID().toString(); // the OTP maps are static, so every test uses its own user

        UserCredentialsProjection credentials = mock(UserCredentialsProjection.class);
        when(credentials.getSalt()).thenReturn(SALT);
        when(credentials.getPasswordHash()).thenReturn(hashPassword("pw", SALT));
        when(credentials.getPhoneNumber()).thenReturn(STORED_PHONE);
        when(userRepository.findBasicCredentialsByUserId(userId)).thenReturn(credentials);
        StudentEntity user = new StudentEntity();
        user.setUserId(userId);
        when(userRepository.findUserByUserId(userId)).thenReturn(user);
    }

    @SuppressWarnings("unchecked")
    private String sentOtp() {
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<String>> phones = ArgumentCaptor.forClass(List.class);
        verify(smsSender).sendSms(otp.capture(), phones.capture());
        assertEquals(List.of(STORED_PHONE), phones.getValue());
        return otp.getValue();
    }

    @Test
    void sendOtpWithoutPasswordLoginIsRefused() {
        assertFalse(userService.sendOtp(userId, "0509999999").isSuccess());
        verifyNoInteractions(smsSender);
    }

    @Test
    void verifyOtpWithoutPasswordLoginIsRefused() {
        assertFalse(userService.verifyOtp(userId, "AAAAA").isSuccess());
    }

    @Test
    void verifyWithoutSentCodeFailsCleanly() {
        assertTrue(userService.loginUser(userId, "pw").isSuccess());
        LoginResponse response = userService.verifyOtp(userId, "AAAAA");
        assertFalse(response.isSuccess());
    }

    @Test
    void wrongPasswordDoesNotUnlockSendOtp() {
        assertFalse(userService.loginUser(userId, "wrong").isSuccess());
        assertFalse(userService.sendOtp(userId, null).isSuccess());
        verifyNoInteractions(smsSender);
    }

    @Test
    void sendsToStoredPhoneAndIgnoresTheClientPhone() {
        assertTrue(userService.loginUser(userId, "pw").isSuccess());
        assertTrue(userService.sendOtp(userId, "0509999999").isSuccess());
        String otp = sentOtp();
        assertEquals(5, otp.length());
        assertTrue(userService.verifyOtp(userId, otp.toLowerCase()).isSuccess());
        // single use: the marker and the code are gone
        assertFalse(userService.verifyOtp(userId, otp).isSuccess());
    }

    @Test
    void fiveWrongCodesLockTheLogin() {
        assertTrue(userService.loginUser(userId, "pw").isSuccess());
        assertTrue(userService.sendOtp(userId, null).isSuccess());
        String otp = sentOtp();
        for (int i = 0; i < 5; i++) {
            assertFalse(userService.verifyOtp(userId, "WRONG").isSuccess());
        }
        assertFalse(userService.verifyOtp(userId, otp).isSuccess());
        assertFalse(userService.sendOtp(userId, null).isSuccess());
    }
}
