package com.lifetracker.infrastructure;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Ledger endpoints over HTTP (MockMvc, real Postgres). Accounts and movement transactions,
 * owner-scoped to the token; balances computed from postings; and the RFC 7807 error mappings.
 */
@AutoConfigureMockMvc
class LedgerEndpointsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String register(String email) throws Exception {
        String creds = "{\"email\":\"" + email + "\",\"password\":\"correct horse battery\"}";
        MvcResult r = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(creds))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.accessToken");
    }

    private String createAccount(String token, String name, String kind, String currency) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"kind\":\"" + kind + "\",\"currency\":\"" + currency + "\"}";
        MvcResult r = mvc.perform(post("/accounts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance.amount").value("0.0000"))
                .andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    private static String movement(String date, String from, String to, String amount, String currency) {
        return "{\"date\":\"" + date + "\",\"from\":\"" + from + "\",\"to\":\"" + to + "\","
                + "\"amount\":{\"amount\":\"" + amount + "\",\"currency\":\"" + currency + "\"}}";
    }

    private void move(String token, String date, String from, String to, String amount) throws Exception {
        mvc.perform(post("/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(movement(date, from, to, amount, "USD")))
                .andExpect(status().isCreated());
    }

    @Test
    void opening_balance_then_spend_yields_correct_balances() throws Exception {
        String token = register("ledger-flow@example.com");
        String equity = createAccount(token, "Opening Balances", "EQUITY", "USD");
        String bank = createAccount(token, "Bank", "ASSET", "USD");
        String groceries = createAccount(token, "Groceries", "EXPENSE", "USD");

        move(token, "2026-07-01", equity, bank, "1000.00");   // opening balance
        move(token, "2026-07-02", bank, groceries, "50.00");  // spend

        mvc.perform(get("/accounts/" + bank).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.balance.amount").value("950.0000"))
                .andExpect(jsonPath("$.balance.currency").value("USD"));
        mvc.perform(get("/accounts/" + groceries).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.balance.amount").value("50.0000"));

        mvc.perform(get("/accounts").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(3));

        mvc.perform(get("/transactions").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value("2026-07-02")); // newest first

        mvc.perform(get("/transactions").param("accountId", groceries).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void a_movement_is_booked_as_a_credit_and_a_debit() throws Exception {
        String token = register("ledger-postings@example.com");
        String bank = createAccount(token, "Bank", "ASSET", "USD");
        String groceries = createAccount(token, "Groceries", "EXPENSE", "USD");

        mvc.perform(post("/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(movement("2026-07-02", bank, groceries, "50.00", "USD")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postings.length()").value(2))
                .andExpect(jsonPath("$.postings[*].direction", containsInAnyOrder("CREDIT", "DEBIT")));
    }

    @Test
    void rejects_a_movement_to_the_same_account() throws Exception {
        String token = register("ledger-same@example.com");
        String bank = createAccount(token, "Bank", "ASSET", "USD");
        mvc.perform(post("/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(movement("2026-07-02", bank, bank, "10.00", "USD")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SAME_ACCOUNT"));
    }

    @Test
    void a_cross_currency_movement_without_the_second_amount_is_422() throws Exception {
        String token = register("ledger-fx@example.com");
        String usd = createAccount(token, "USD Bank", "ASSET", "USD");
        String eur = createAccount(token, "EUR Bank", "ASSET", "EUR");
        mvc.perform(post("/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(movement("2026-07-02", usd, eur, "10.00", "USD")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CONVERTED_AMOUNT_REQUIRED"));
    }

    @Test
    void records_a_cross_currency_movement_and_reports_the_derived_rate() throws Exception {
        String token = register("ledger-fx2@example.com");
        String usd = createAccount(token, "USD Bank", "ASSET", "USD");
        String eur = createAccount(token, "EUR Bank", "ASSET", "EUR");
        String body = "{\"date\":\"2026-07-02\",\"from\":\"" + usd + "\",\"to\":\"" + eur + "\","
                + "\"amount\":{\"amount\":\"100.00\",\"currency\":\"USD\"},"
                + "\"toAmount\":{\"amount\":\"90.00\",\"currency\":\"EUR\"}}";
        mvc.perform(post("/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postings.length()").value(2))
                .andExpect(jsonPath("$.exchangeRate").value("0.90000000"));

        // The USD leg reads -100 (money left), the EUR account reads +90 (money arrived).
        mvc.perform(get("/accounts/" + usd).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.balance.amount").value("-100.0000"));
        mvc.perform(get("/accounts/" + eur).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.balance.amount").value("90.0000"));
    }

    @Test
    void rejects_a_reference_to_an_unknown_account() throws Exception {
        String token = register("ledger-unknown@example.com");
        String bank = createAccount(token, "Bank", "ASSET", "USD");
        mvc.perform(post("/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movement("2026-07-02", UUID.randomUUID().toString(), bank, "10.00", "USD")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void rejects_an_unknown_account_kind() throws Exception {
        String token = register("ledger-kind@example.com");
        mvc.perform(post("/accounts").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"kind\":\"BOGUS\",\"currency\":\"USD\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    void getting_a_missing_account_is_404() throws Exception {
        String token = register("ledger-404@example.com");
        mvc.perform(get("/accounts/" + UUID.randomUUID()).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void the_ledger_requires_authentication() throws Exception {
        mvc.perform(get("/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(post("/transactions")).andExpect(status().isUnauthorized());
    }

    @Test
    void a_body_with_an_unknown_field_is_rejected_not_silently_dropped() throws Exception {
        String token = register("strict-body@example.com");
        // 'colour' is not in CreateAccountRequest. The spec's additionalProperties:false says reject a
        // typo, never drop it -- a dropped field compiles, sends, and does nothing. (Global rule; any
        // request body would do -- accounts just has the handiest harness here.)
        String body = "{\"name\":\"Bank\",\"kind\":\"ASSET\",\"currency\":\"USD\",\"colour\":\"blue\"}";
        mvc.perform(post("/accounts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
