package org.example.learningprojectserver.security;

import org.example.learningprojectserver.response.BasicResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns a failed @PreAuthorize ownership check into 403 with the usual BasicResponse body.
 * Highest precedence so a catch-all exception handler elsewhere cannot turn it into 500.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessDeniedAdvice {

    public static final String ACCESS_DENIED_MESSAGE = "אין לך הרשאה לבצע פעולה זו";

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BasicResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        String message = ex instanceof FeatureDisabledException ? ex.getMessage() : ACCESS_DENIED_MESSAGE;
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new BasicResponse<>(false, message));
    }
}
