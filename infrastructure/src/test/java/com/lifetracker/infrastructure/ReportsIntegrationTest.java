package com.lifetracker.infrastructure;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The reporting endpoints over HTTP (MockMvc, real Postgres). Builds a small Book and checks net worth
 * (Assets − Liabilities), spending, and income — proving transfers and a borrow stay out of
 * spending/income, and that receivable/payable-style Liability accounts land in net worth.
 */
@AutoConfigureMockMvc
class ReportsIntegrationTest extends AbstractIntegrationTest {

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

    private String createAccount(String token, String name, String kind) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"kind\":\"" + kind + "\",\"currency\":\"USD\"}";
        MvcResult r = mvc.perform(post("/accounts").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    private void move(String token, String date, String from, String to, String amount) throws Exception {
        String body = "{\"date\":\"" + date + "\",\"from\":\"" + from + "\",\"to\":\"" + to + "\","
                + "\"amount\":{\"amount\":\"" + amount + "\",\"currency\":\"USD\"}}";
        mvc.perform(post("/transactions").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void net_worth_spending_and_income() throws Exception {
        String token = register("reports@example.com");
        String equity = createAccount(token, "Opening", "EQUITY");
        String bank = createAccount(token, "Bank", "ASSET");
        String card = createAccount(token, "Card", "LIABILITY");
        String salary = createAccount(token, "Salary", "INCOME");
        String groceries = createAccount(token, "Groceries", "EXPENSE");

        move(token, "2026-07-01", equity, bank, "1000.00");     // opening balance -> bank +1000
        move(token, "2026-07-05", salary, bank, "2000.00");     // income          -> bank +2000
        move(token, "2026-07-10", bank, groceries, "150.00");   // spend           -> bank -150
        move(token, "2026-07-15", card, bank, "500.00");        // borrow on card  -> bank +500, owe 500

        // Assets = Bank 3350; Liabilities = Card 500; net worth = 2850.
        mvc.perform(get("/reports/net-worth").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byCurrency.length()").value(1))
                .andExpect(jsonPath("$.byCurrency[0].currency").value("USD"))
                .andExpect(jsonPath("$.byCurrency[0].assets.amount").value("3350.0000"))
                .andExpect(jsonPath("$.byCurrency[0].liabilities.amount").value("500.0000"))
                .andExpect(jsonPath("$.byCurrency[0].netWorth.amount").value("2850.0000"));

        // Spending: only the $150 groceries -- the transfer and the borrow touch no Expense account.
        mvc.perform(get("/reports/spending").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byAccount.length()").value(1))
                .andExpect(jsonPath("$.byAccount[0].name").value("Groceries"))
                .andExpect(jsonPath("$.byAccount[0].amount.amount").value("150.0000"))
                .andExpect(jsonPath("$.totals[0].amount.amount").value("150.0000"));

        // Income: only the $2000 salary.
        mvc.perform(get("/reports/income").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byAccount[0].name").value("Salary"))
                .andExpect(jsonPath("$.byAccount[0].amount.amount").value("2000.0000"))
                .andExpect(jsonPath("$.totals[0].amount.amount").value("2000.0000"));

        // A range after all the activity is empty.
        mvc.perform(get("/reports/spending").param("from", "2026-08-01").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byAccount.length()").value(0))
                .andExpect(jsonPath("$.totals.length()").value(0));
    }

    @Test
    void reports_require_authentication() throws Exception {
        mvc.perform(get("/reports/net-worth")).andExpect(status().isUnauthorized());
        mvc.perform(get("/reports/spending")).andExpect(status().isUnauthorized());
    }
}
