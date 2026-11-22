package org.example.learningprojectserver.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns a failed @PreAuthorize ownership check (or a disabled feature) into a 403 RFC 7807 problem,
 * with the same {@code success} / {@code errorCode} compatibility fields as GlobalExceptionHandler.
 * Highest precedence so a catch-all exception handler elsewhere cannot turn it into 500.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessDeniedAdvice {

    public static final String ACCESS_DENIED_MESSAGE = "אין לך הרשאה לבצע פעולה זו";

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        String message = ex instanceof FeatureDisabledException ? ex.getMessage() : ACCESS_DENIED_MESSAGE;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, message);
        problem.setTitle("אין הרשאה");
        problem.setProperty("success", false);
        problem.setProperty("errorCode", message);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }
}
