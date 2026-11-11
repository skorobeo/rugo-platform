package ru.rugo.admin.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.rugo.admin.stats.AdminDashboardService;
import ru.rugo.admin.web.dto.AdminDashboardDto;

/** Сводный дашборд веб-админки. Доступ — {@link AdminAuthFilter}. */
@RestController
@RequestMapping("/admin/api/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping
    public AdminDashboardDto dashboard(@RequestParam(defaultValue = "7d") String period) {
        return dashboardService.dashboard(period);
    }
}
