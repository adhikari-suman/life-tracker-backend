package com.lifetracker.infrastructure.web;

import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.infrastructure.persistence.report.AccountAmountView;
import com.lifetracker.infrastructure.persistence.report.LabelAmountView;
import com.lifetracker.infrastructure.persistence.report.ReportQueryService;
import com.lifetracker.infrastructure.web.dto.AccountAmountResponse;
import com.lifetracker.infrastructure.web.dto.ActivityReportResponse;
import com.lifetracker.infrastructure.web.dto.CurrencyAmountResponse;
import com.lifetracker.infrastructure.web.dto.LabelAmountResponse;
import com.lifetracker.infrastructure.web.dto.CurrencyNetWorthResponse;
import com.lifetracker.infrastructure.web.dto.MoneyDto;
import com.lifetracker.infrastructure.web.dto.NetWorthResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computed-on-demand reports (ADR-0013): net worth (Assets − Liabilities), and spending / income over
 * a date range. Owner-scoped from the token; thin — call the query service, map, aggregate totals.
 */
@RestController
@RequestMapping("/reports")
class ReportController {

    private final ReportQueryService reports;

    ReportController(ReportQueryService reports) {
        this.reports = reports;
    }

    @GetMapping("/net-worth")
    NetWorthResponse netWorth(@AuthenticationPrincipal Jwt jwt) {
        List<CurrencyNetWorthResponse> byCurrency = reports.netWorth(AuthPrincipal.ownerId(jwt)).stream()
                .map(v -> new CurrencyNetWorthResponse(v.currency(),
                        money(v.assets(), v.currency()),
                        money(v.liabilities(), v.currency()),
                        money(v.netWorth(), v.currency())))
                .toList();
        return new NetWorthResponse(byCurrency);
    }

    @GetMapping("/spending")
    ActivityReportResponse spending(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        OwnerId owner = AuthPrincipal.ownerId(jwt);
        return activityReport(from, to, reports.spending(owner, from, to), reports.spendingByLabel(owner, from, to));
    }

    @GetMapping("/income")
    ActivityReportResponse income(@AuthenticationPrincipal Jwt jwt,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        OwnerId owner = AuthPrincipal.ownerId(jwt);
        return activityReport(from, to, reports.income(owner, from, to), reports.incomeByLabel(owner, from, to));
    }

    /**
     * The same money twice: {@code byAccount} is where it was booked, {@code byLabel} what it was for.
     * Totals are summed from {@code byAccount}, and summing every {@code byLabel} row's {@code own}
     * must give the identical per-currency figure — the check that catches a roll-up bug.
     */
    private static ActivityReportResponse activityReport(LocalDate from, LocalDate to,
                                                         List<AccountAmountView> byAccount,
                                                         List<LabelAmountView> byLabel) {
        List<AccountAmountResponse> accounts = byAccount.stream()
                .map(v -> new AccountAmountResponse(v.accountId(), v.name(), v.currency(), money(v.amount(), v.currency())))
                .toList();

        List<LabelAmountResponse> labels = byLabel.stream()
                .map(v -> new LabelAmountResponse(v.labelId(), v.name(), v.path(), v.parentLabelId(), v.currency(),
                        money(v.own(), v.currency()), money(v.rolledUp(), v.currency())))
                .toList();

        Map<String, BigDecimal> totalByCurrency = new LinkedHashMap<>();
        for (AccountAmountView v : byAccount) {
            totalByCurrency.merge(v.currency(), v.amount(), BigDecimal::add);
        }
        List<CurrencyAmountResponse> totals = totalByCurrency.entrySet().stream()
                .map(e -> new CurrencyAmountResponse(e.getKey(), money(e.getValue(), e.getKey())))
                .toList();

        return new ActivityReportResponse(from, to, accounts, labels, totals);
    }

    private static MoneyDto money(BigDecimal amount, String currency) {
        return new MoneyDto(amount.toPlainString(), currency);
    }
}
