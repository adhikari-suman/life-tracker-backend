package com.lifetracker.infrastructure.web;

import com.lifetracker.application.user.SendEmailVerification;
import com.lifetracker.application.user.VerifyEmail;
import com.lifetracker.infrastructure.web.dto.VerifyEmailRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Email verification: consume a token from the emailed link (public), or resend the link to the
 * signed-in User (authenticated). Thin — parse, call the use case. Bad tokens map to 400 in
 * {@link ApiExceptionHandler}, so the token body is deliberately not bean-validated.
 */
@RestController
@RequestMapping("/auth/verify-email")
class EmailVerificationController {

    private final VerifyEmail verifyEmail;
    private final SendEmailVerification sendEmailVerification;

    EmailVerificationController(VerifyEmail verifyEmail, SendEmailVerification sendEmailVerification) {
        this.verifyEmail = verifyEmail;
        this.sendEmailVerification = sendEmailVerification;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verify(@RequestBody VerifyEmailRequest request) {
        verifyEmail.execute(request.token());
    }

    @PostMapping("/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void resend(@AuthenticationPrincipal Jwt jwt) {
        sendEmailVerification.execute(AuthPrincipal.userId(jwt));
    }
}
