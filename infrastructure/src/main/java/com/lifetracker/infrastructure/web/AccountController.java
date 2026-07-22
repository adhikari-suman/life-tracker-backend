package com.lifetracker.infrastructure.web;

import com.lifetracker.application.account.OpenAccount;
import com.lifetracker.application.account.OpenAccountCommand;
import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.infrastructure.persistence.account.AccountQueryService;
import com.lifetracker.infrastructure.persistence.account.AccountView;
import com.lifetracker.infrastructure.web.dto.AccountResponse;
import com.lifetracker.infrastructure.web.dto.CreateAccountRequest;
import com.lifetracker.infrastructure.web.dto.MoneyDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The caller's Ledger accounts. Create (write use case), list, and get (read via query service, each
 * with a computed balance). Owner-scoped from the token. Thin — parse, call, map.
 */
@RestController
@RequestMapping("/accounts")
class AccountController {

    private final OpenAccount openAccount;
    private final AccountQueryService query;

    AccountController(OpenAccount openAccount, AccountQueryService query) {
        this.openAccount = openAccount;
        this.query = query;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AccountResponse create(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateAccountRequest request) {
        OwnerId owner = AuthPrincipal.ownerId(jwt);
        AccountId id = openAccount.execute(
                new OpenAccountCommand(owner, request.name(), request.kind(), request.currency()));
        return query.findById(owner, id).map(AccountController::toResponse)
                .orElseThrow(() -> new IllegalStateException("account vanished after creation: " + id.value()));
    }

    @GetMapping
    List<AccountResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return query.findByOwner(AuthPrincipal.ownerId(jwt)).stream().map(AccountController::toResponse).toList();
    }

    @GetMapping("/{accountId}")
    AccountResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID accountId) {
        return query.findById(AuthPrincipal.ownerId(jwt), AccountId.of(accountId))
                .map(AccountController::toResponse)
                .orElseThrow(AccountNotFoundException::new);
    }

    private static AccountResponse toResponse(AccountView view) {
        return new AccountResponse(view.id(), view.name(), view.kind(), view.currency(),
                new MoneyDto(view.balance().toPlainString(), view.currency()));
    }
}
