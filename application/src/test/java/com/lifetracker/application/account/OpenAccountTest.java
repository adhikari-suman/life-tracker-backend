package com.lifetracker.application.account;

import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.account.AccountKind;
import com.lifetracker.domain.account.InvalidAccountNameException;
import com.lifetracker.domain.ledger.OwnerId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAccountTest {

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final OpenAccount openAccount = new OpenAccount(accounts);
    private final OwnerId owner = OwnerId.of(UUID.randomUUID());

    @Test
    void opens_an_account() {
        AccountId id = openAccount.execute(new OpenAccountCommand(owner, "Checking", "ASSET", "USD"));

        assertTrue(accounts.findById(owner, id).isPresent());
        assertEquals(AccountKind.ASSET, accounts.findById(owner, id).orElseThrow().kind());
    }

    @Test
    void rejects_an_unknown_kind() {
        assertThrows(InvalidAccountException.class,
                () -> openAccount.execute(new OpenAccountCommand(owner, "X", "BOGUS", "USD")));
    }

    @Test
    void rejects_an_unknown_currency() {
        assertThrows(InvalidAccountException.class,
                () -> openAccount.execute(new OpenAccountCommand(owner, "X", "ASSET", "ZZZ")));
    }

    @Test
    void rejects_a_blank_name() {
        assertThrows(InvalidAccountNameException.class,
                () -> openAccount.execute(new OpenAccountCommand(owner, "   ", "ASSET", "USD")));
    }
}
