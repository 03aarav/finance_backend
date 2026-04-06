package com.finance_backned.finance.Controller;



import com.finance_backned.finance.Model.Record;
import com.finance_backned.finance.Model.SummarySnapshot;

import com.finance_backned.finance.ServiceImp.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final SummaryService summaryService;

    // Total income, total expense, net balance — all roles
    // GET /dashboard/summary
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public SummarySnapshot getSummary() {
        return summaryService.getOverallSummary();
    }

    // Category-wise totals for a given period — analyst + admin
    // GET /dashboard/categories?periodType=month&periodKey=2025-03
    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public List<SummarySnapshot> getCategoryTotals(
            @RequestParam(defaultValue = "month") String periodType,
            @RequestParam String periodKey
    ) {
        return summaryService.getCategoryTotals(periodType, periodKey);
    }

    // Monthly or weekly trends — analyst + admin
    // GET /dashboard/trends?periodType=month
    // GET /dashboard/trends?periodType=week
    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public List<SummarySnapshot> getTrends(
            @RequestParam(defaultValue = "month") String periodType
    ) {
        if (!periodType.equals("week") && !periodType.equals("month") && !periodType.equals("year")) {
            throw new com.finance_backned.finance.ExceptionHandler.BadRequestException(
                    "periodType must be week, month, or year"
            );
        }
        return summaryService.getTrends(periodType);
    }

    // Recent activity — all roles
    // GET /dashboard/recent?limit=10
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public List<Record> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit
    ) {
        if (limit > 50) limit = 50; // cap it
        return summaryService.getRecentActivity(limit);
    }

    @PostMapping("/recalculate")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> recalculate() {
        long startTime = System.currentTimeMillis();

        String result = summaryService.recalculateAll();

        long duration = System.currentTimeMillis() - startTime;

        return Map.of(
                "status", "success",
                "message", result,
                "durationMs", duration,
                "timestamp", java.time.LocalDateTime.now().toString()
        );
    }
}
