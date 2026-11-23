package org.example.learningprojectserver.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.entities.UserEntity;
import org.example.learningprojectserver.enums.AuditAction;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.AccessGuard;
import org.example.learningprojectserver.security.AuthCookies;
import org.example.learningprojectserver.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Support impersonation: a system admin gets a short-lived token that acts as another user and
 * carries the admin's id, so every request (and every audit entry) shows who is really behind it.
 * Start and stop are written to the audit log. The impersonation token replaces only the httpOnly
 * access cookie: the admin's refresh cookie stays, so when the 30 minutes end a refresh brings the
 * admin back to their own session.
 */
@Service
@RequiredArgsConstructor
public class ImpersonationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final AccessGuard accessGuard;
    private final AuthTokenService authTokenService;
    private final AuthCookies authCookies;

    public BasicResponse<Map<String, String>> start(String targetUserId, HttpServletResponse httpResponse) {
        if (CurrentUser.impersonatorId() != null) {
            return new BasicResponse<>(false, "כבר מתבצעת התחזות. יש לסיים אותה לפני התחזות חדשה");
        }
        if (targetUserId == null || targetUserId.isBlank()) {
            return new BasicResponse<>(false, "יש להזין תעודת זהות של משתמש");
        }
        String adminId = CurrentUser.userId();
        if (targetUserId.equals(adminId)) {
            return new BasicResponse<>(false, "לא ניתן להתחזות לעצמך");
        }
        UserEntity target = userRepository.findUserByUserId(targetUserId);
        if (target == null) {
            return new BasicResponse<>(false, "המשתמש לא נמצא");
        }
        if (target.getRole() == Role.SYSTEM_ADMIN) {
            return new BasicResponse<>(false, "לא ניתן להתחזות למנהל מערכת");
        }

        String token = jwtService.generateImpersonationToken(target.getUserId(), target.getUsername(),
                target.getRole().name(), adminId, CurrentUser.username());

        auditService.record(AuditAction.IMPERSONATION_STARTED, "USER", target.getUserId(),
                null, "role=" + target.getRole(),
                accessGuard.schoolCodeOf(target.getUserId(), target.getRole().name()));

        authCookies.writeAccessOnly(httpResponse, token, Duration.ofMinutes(30));

        Map<String, String> data = new LinkedHashMap<>();
        data.put("userId", target.getUserId());
        data.put("username", target.getUsername());
        data.put("role", target.getRole().name());
        BasicResponse<Map<String, String>> response =
                new BasicResponse<>(true, "התחזות למשתמש " + target.getUsername() + " החלה");
        response.setData(data);
        return response;
    }

    /** Ends impersonation and writes fresh session cookies for the admin who started it. */
    public BasicResponse<Map<String, String>> stop(HttpServletResponse httpResponse) {
        String adminId = CurrentUser.impersonatorId();
        if (adminId == null) {
            return new BasicResponse<>(false, "אין התחזות פעילה");
        }
        UserEntity admin = userRepository.findUserByUserId(adminId);
        if (admin == null || admin.getRole() != Role.SYSTEM_ADMIN) {
            return new BasicResponse<>(false, "מנהל המערכת שהתחיל את ההתחזות לא נמצא");
        }

        auditService.record(AuditAction.IMPERSONATION_STOPPED, "USER", CurrentUser.userId(),
                "role=" + CurrentUser.role(), null,
                accessGuard.schoolCodeOf(CurrentUser.userId(), CurrentUser.role()));

        authCookies.write(httpResponse,
                authTokenService.issue(admin.getUserId(), admin.getUsername(), Role.SYSTEM_ADMIN.name()));
        Map<String, String> data = new LinkedHashMap<>();
        data.put("userId", admin.getUserId());
        BasicResponse<Map<String, String>> response = new BasicResponse<>(true, "ההתחזות הסתיימה");
        response.setData(data);
        return response;
    }
}
