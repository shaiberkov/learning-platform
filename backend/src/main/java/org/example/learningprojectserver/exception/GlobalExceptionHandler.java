package org.example.learningprojectserver.exception;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns errors into RFC 7807 {@link ProblemDetail} bodies (application/problem+json) with a Hebrew {@code detail}.
 * <p>
 * Successful calls keep returning {@code BasicResponse}; only requests that fail validation, hit a framework error
 * (missing parameter, wrong type, unknown path...) or throw unexpectedly end up here. Besides the standard fields,
 * every problem carries {@code success=false} and {@code errorCode} (= the Hebrew detail) so code that reads the
 * BasicResponse shape still finds a message, and validation problems carry {@code errors}: field name to message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static final URI TYPE_VALIDATION = URI.create("urn:learning-app:problem:validation");
    public static final URI TYPE_BAD_REQUEST = URI.create("urn:learning-app:problem:bad-request");
    public static final URI TYPE_FORBIDDEN = URI.create("urn:learning-app:problem:forbidden");
    public static final URI TYPE_INTERNAL = URI.create("urn:learning-app:problem:internal");

    public static final String VALIDATION_TITLE = "הבקשה אינה תקינה";
    public static final String VALIDATION_DETAIL = "חלק מהשדות שנשלחו אינם תקינים";
    public static final String INTERNAL_DETAIL = "אירעה שגיאה בלתי צפויה בשרת. נסו שוב בעוד מספר רגעים.";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        return validationProblem(errors, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status,
                                                                            WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getAllValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                String field = error instanceof FieldError fieldError ? fieldError.getField() : name;
                errors.putIfAbsent(field == null ? "request" : field, error.getDefaultMessage());
            }
        });
        return validationProblem(errors, headers);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers, HttpStatusCode status,
                                                                          WebRequest request) {
        Map<String, String> errors = Map.of(ex.getParameterName(), "שדה חובה חסר");
        return validationProblem(errors, headers);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String field = ex.getPropertyName() == null ? "request" : ex.getPropertyName();
        return validationProblem(Map.of(field, "הערך שנשלח אינו בפורמט הנכון"), headers);
    }

    /** Covers @Validated service/bean method validation, which reports through ConstraintViolationException. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            errors.putIfAbsent(field, violation.getMessage());
        });
        return validationProblem(errors, new HttpHeaders());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = problem(HttpStatus.FORBIDDEN, TYPE_FORBIDDEN, "אין הרשאה", "אין לך הרשאה לבצע פעולה זו");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletResponse response) {
        log.error("Unhandled exception", ex);
        if (response.isCommitted()) {
            // e.g. an SSE stream that already started: nothing useful can be written any more.
            return null;
        }
        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, TYPE_INTERNAL, "שגיאת שרת", INTERNAL_DETAIL);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    /**
     * Every other framework exception (404 no handler, 405, 415, malformed JSON...) is already a ProblemDetail;
     * give it a Hebrew detail and the compatibility fields.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.updateAndGetBody(getMessageSource(), java.util.Locale.getDefault());
        }
        if (body instanceof ProblemDetail problem && !(problem.getProperties() != null
                && problem.getProperties().containsKey("errorCode"))) {
            problem.setDetail(hebrewDetailFor(statusCode));
            if (problem.getType() == null || URI.create("about:blank").equals(problem.getType())) {
                problem.setType(TYPE_BAD_REQUEST);
            }
            addCompatibilityFields(problem);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private ResponseEntity<Object> validationProblem(Map<String, String> errors, HttpHeaders headers) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, TYPE_VALIDATION, VALIDATION_TITLE,
                errors.size() == 1 ? errors.values().iterator().next() : VALIDATION_DETAIL);
        problem.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(problem);
    }

    private static ProblemDetail problem(HttpStatus status, URI type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);
        addCompatibilityFields(problem);
        return problem;
    }

    private static void addCompatibilityFields(ProblemDetail problem) {
        problem.setProperty("success", false);
        problem.setProperty("errorCode", problem.getDetail());
    }

    private static String hebrewDetailFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "הבקשה אינה תקינה";
            case 401 -> "יש להתחבר מחדש";
            case 403 -> "אין לך הרשאה לבצע פעולה זו";
            case 404 -> "הכתובת המבוקשת לא נמצאה";
            case 405 -> "סוג הבקשה אינו נתמך בכתובת זו";
            case 406, 415 -> "פורמט הבקשה אינו נתמך";
            case 413 -> "הבקשה גדולה מדי";
            case 503 -> "השירות אינו זמין כרגע. נסו שוב בעוד מספר רגעים.";
            default -> status.is5xxServerError() ? INTERNAL_DETAIL : "הבקשה נכשלה";
        };
    }
}
