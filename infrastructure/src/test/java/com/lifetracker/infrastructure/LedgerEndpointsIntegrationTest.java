package com.lifetracker.infrastructure;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
        return "{\"date\":\"" + date + "\",\"time\":\"12:00\",\"from\":\"" + from + "\",\"to\":\"" + to + "\","
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

    // ADR-0018. The spec constrains `time` with pattern ^([01][0-9]|2[0-3]):[0-5][0-9]$ in BOTH
    // directions, and marks it required. Jackson's ISO default for a LocalTime does neither: it
    // emits 19:42:00, and an omitted field arrives as a null that used to reach the use case and
    // NPE into a 500. Both are pinned here because both were live defects against a green suite.
    @Test
    void a_transaction_time_is_HH_mm_on_the_way_out_never_with_seconds() throws Exception {
        String token = register("ledger-time-format@example.com");
        String bank = createAccount(token, "Bank", "ASSET", "USD");
        String groceries = createAccount(token, "Groceries", "EXPENSE", "USD");

        String body = "{\"date\":\"2026-07-02\",\"time\":\"19:42\",\"from\":\"" + bank + "\",\"to\":\"" + groceries
                + "\",\"amount\":{\"amount\":\"12.50\",\"currency\":\"USD\"}}";
        mvc.perform(post("/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.time").value("19:42"));

        mvc.perform(get("/transactions").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$[0].time").value("19:42"));
    }

    @Test
    void a_movement_without_a_time_is_422_not_500() throws Exception {
        String token = register("ledger-time-missing@example.com");
        String bank = createAccount(token, "Bank", "ASSET", "USD");
        String groceries = createAccount(token, "Groceries", "EXPENSE", "USD");

        // The body parses; it just omits a required field. 400 is reserved for a body that cannot
        // be read AS WRITTEN (malformed JSON, or a field the schema forbids).
        String body = "{\"date\":\"2026-07-02\",\"from\":\"" + bank + "\",\"to\":\"" + groceries
                + "\",\"amount\":{\"amount\":\"12.50\",\"currency\":\"USD\"}}";
        mvc.perform(post("/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    void a_time_carrying_an_offset_is_refused() throws Exception {
        String token = register("ledger-time-offset@example.com");
        String bank = createAccount(token, "Bank", "ASSET", "USD");
        String groceries = createAccount(token, "Groceries", "EXPENSE", "USD");

        // An offset is precisely what Occurred At must not carry — accepting one would let a
        // late-evening purchase drift into the next day and change which month it reports in.
        String body = "{\"date\":\"2026-07-02\",\"time\":\"19:42:00+05:00\",\"from\":\"" + bank + "\",\"to\":\""
                + groceries + "\",\"amount\":{\"amount\":\"12.50\",\"currency\":\"USD\"}}";
        mvc.perform(post("/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void within_a_day_the_list_is_ordered_by_time_descending() throws Exception {
        String token = register("ledger-time-order@example.com");
        String bank = createAccount(token, "Bank", "ASSET", "USD");
        String groceries = createAccount(token, "Groceries", "EXPENSE", "USD");

        // Recorded out of order on purpose: the list must read in the order things HAPPENED, not
        // the order they were typed (ADR-0018), so created_at is a tiebreak and nothing more.
        at(token, "2026-07-02", "08:15", bank, groceries, "3.20");
        at(token, "2026-07-02", "22:05", bank, groceries, "9.99");
        at(token, "2026-07-02", "19:42", bank, groceries, "12.50");

        mvc.perform(get("/transactions").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$[0].time").value("22:05"))
                .andExpect(jsonPath("$[1].time").value("19:42"))
                .andExpect(jsonPath("$[2].time").value("08:15"));
    }

    private void at(String token, String date, String time, String from, String to, String amount) throws Exception {
        String body = "{\"date\":\"" + date + "\",\"time\":\"" + time + "\",\"from\":\"" + from + "\",\"to\":\"" + to
                + "\",\"amount\":{\"amount\":\"" + amount + "\",\"currency\":\"USD\"}}";
        mvc.perform(post("/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
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
        String body = "{\"date\":\"2026-07-02\",\"time\":\"12:00\",\"from\":\"" + usd + "\",\"to\":\"" + eur + "\","
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

    // The companion to the rule above, and a BLANKET one: a body that parses and only then omits a
    // required field is 422 VALIDATION. It has to hold across the surface rather than wherever
    // someone remembered @Valid, which is exactly how it failed -- each of these 500'd on a null
    // reaching the domain, while /accounts missing `kind` answered 422 purely because the enum
    // failed to deserialize. Same client mistake, three different answers.
    @Test
    void a_missing_required_field_is_422_on_every_body() throws Exception {
        String token = register("required-fields@example.com");

        record Case(String path, String body) { }
        var cases = List.of(
                new Case("/accounts", "{\"kind\":\"ASSET\",\"currency\":\"USD\"}"),
                new Case("/accounts", "{\"name\":\"Bank\",\"currency\":\"USD\"}"),
                new Case("/accounts", "{\"name\":\"Bank\",\"kind\":\"ASSET\"}"),
                new Case("/labels", "{}"));

        for (Case c : cases) {
            mvc.perform(post(c.path()).header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content(c.body()))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("VALIDATION"));
        }
    }

    @Test
    void setting_a_posting_label_with_no_labelId_is_422() throws Exception {
        String token = register("posting-label-required@example.com");
        // Clearing a label is DELETE on this sub-resource, so a PUT of nothing is malformed rather
        // than a quiet way to ask for removal.
        mvc.perform(put("/postings/{id}/label", UUID.randomUUID()).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }
}
