package com.lifetracker.infrastructure.web.dto;

import java.util.UUID;

/** Wire response for one posting — the account, its direction (DEBIT/CREDIT), and the amount. */
public record PostingResponse(UUID accountId, String direction, MoneyDto amount) {
}
