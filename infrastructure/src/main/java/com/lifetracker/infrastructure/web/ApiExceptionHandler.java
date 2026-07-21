package com.lifetracker.infrastructure.web;

import com.lifetracker.application.session.InvalidRefreshTokenException;
import com.lifetracker.application.session.SessionNotFoundException;
import com.lifetracker.application.user.EmailAlreadyRegisteredException;
import com.lifetracker.application.user.InvalidCredentialsException;
import com.lifetracker.domain.user.InvalidEmailException;
import com.lifetracker.domain.user.WeakPasswordException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain and application exceptions to RFC 7807 problem responses. The status codes live HERE,
 * at the boundary — the domain does not know what a 422 is. Credential and refresh failures collapse
 * to one indistinguishable 401, so the API never reveals which part was wrong.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler({InvalidCredentialsException.class, InvalidRefreshTokenException.class})
    ProblemDetail unauthorized(RuntimeException e) {
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication failed.");
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail emailTaken(EmailAlreadyRegisteredException e) {
        return problem(HttpStatus.CONFLICT, "EMAIL_TAKEN", "That email is already registered.");
    }

    @ExceptionHandler({InvalidEmailException.class, WeakPasswordException.class})
    ProblemDetail unprocessable(RuntimeException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION", "Request body is invalid.");
    }

    @ExceptionHandler(SessionNotFoundException.class)
    ProblemDetail sessionNotFound(SessionNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found.");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        return problem;
    }
}
