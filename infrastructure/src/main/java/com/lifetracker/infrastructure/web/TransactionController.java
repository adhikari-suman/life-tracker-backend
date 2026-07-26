package com.lifetracker.infrastructure.web;

import com.lifetracker.application.transaction.RecordTransaction;
import com.lifetracker.application.transaction.RecordTransactionCommand;
import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.money.Money;
import com.lifetracker.domain.transaction.TransactionId;
import com.lifetracker.infrastructure.persistence.transaction.TransactionQueryService;
import com.lifetracker.infrastructure.persistence.transaction.TransactionView;
import com.lifetracker.infrastructure.web.dto.MoneyDto;
import com.lifetracker.infrastructure.web.dto.PostingResponse;
import com.lifetracker.infrastructure.web.dto.RecordTransactionRequest;
import com.lifetracker.infrastructure.web.dto.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * The caller's Ledger transactions. Record a movement (write use case), list newest-first, and get one
 * (read via query service, as balanced postings). Owner-scoped from the token. Thin — parse, call, map.
 */
@RestController
@RequestMapping("/transactions")
class TransactionController {

    private final RecordTransaction recordTransaction;
    private final TransactionQueryService query;

    TransactionController(RecordTransaction recordTransaction, TransactionQueryService query) {
        this.recordTransaction = recordTransaction;
        this.query = query;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TransactionResponse record(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RecordTransactionRequest request) {
        OwnerId owner = AuthPrincipal.ownerId(jwt);
        TransactionId id = recordTransaction.execute(new RecordTransactionCommand(
                owner, request.date(), request.time(), AccountId.of(request.from()), AccountId.of(request.to()),
                parseMoney(request.amount()), request.toAmount() != null ? parseMoney(request.toAmount()) : null,
                request.labelId()));
        return query.findById(owner, id).map(TransactionController::toResponse)
                .orElseThrow(() -> new IllegalStateException("transaction vanished after recording: " + id.value()));
    }

    @GetMapping
    List<TransactionResponse> list(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam(name = "accountId", required = false) UUID accountId) {
        return query.findByOwner(AuthPrincipal.ownerId(jwt), accountId).stream()
                .map(TransactionController::toResponse).toList();
    }

    @GetMapping("/{transactionId}")
    TransactionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID transactionId) {
        return query.findById(AuthPrincipal.ownerId(jwt), TransactionId.of(transactionId))
                .map(TransactionController::toResponse)
                .orElseThrow(TransactionNotFoundException::new);
    }

    private static Money parseMoney(MoneyDto dto) {
        if (dto == null || dto.amount() == null || dto.currency() == null) {
            throw new MalformedMoneyException("amount and currency are required");
        }
        try {
            return new Money(new BigDecimal(dto.amount()), Currency.getInstance(dto.currency()));
        } catch (RuntimeException e) {
            throw new MalformedMoneyException("invalid amount or currency");
        }
    }

    private static TransactionResponse toResponse(TransactionView view) {
        List<PostingResponse> postings = view.postings().stream()
                .map(p -> new PostingResponse(p.id(), p.accountId(), p.side(),
                        new MoneyDto(p.amount().toPlainString(), p.currency()), p.labelId()))
                .toList();
        String rate = view.exchangeRate() != null ? view.exchangeRate().toPlainString() : null;
        return new TransactionResponse(view.id(), view.date(), view.time(), rate, postings);
    }
}
