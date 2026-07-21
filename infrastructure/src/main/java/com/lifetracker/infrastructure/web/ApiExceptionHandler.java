package com.lifetracker.infrastructure.web;

import com.lifetracker.application.session.InvalidRefreshTokenException;
import com.lifetracker.application.session.SessionNotFoundException;
import com.lifetracker.application.sharing.CannotShareWithYourselfException;
import com.lifetracker.application.sharing.GranteeNotFoundException;
import com.lifetracker.application.sharing.ViewGrantAlreadyExistsException;
import com.lifetracker.application.sharing.ViewGrantNotFoundException;
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

    @ExceptionHandler(GranteeNotFoundException.class)
    ProblemDetail granteeNotFound(GranteeNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "GRANTEE_NOT_FOUND", "No registered user owns that email.");
    }

    @ExceptionHandler(ViewGrantNotFoundException.class)
    ProblemDetail viewGrantNotFound(ViewGrantNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "VIEW_GRANT_NOT_FOUND", "View grant not found.");
    }

    @ExceptionHandler(ShareLinkNotFoundException.class)
    ProblemDetail shareLinkNotFound(ShareLinkNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "SHARE_LINK_NOT_FOUND", "Link sharing is off.");
    }

    @ExceptionHandler(ViewGrantAlreadyExistsException.class)
    ProblemDetail viewGrantExists(ViewGrantAlreadyExistsException e) {
        return problem(HttpStatus.CONFLICT, "VIEW_GRANT_EXISTS", "That user already has a view grant on this book.");
    }

    @ExceptionHandler(CannotShareWithYourselfException.class)
    ProblemDetail cannotShareWithSelf(CannotShareWithYourselfException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "CANNOT_SHARE_WITH_SELF", "You cannot share a book with yourself.");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        return problem;
    }
}
