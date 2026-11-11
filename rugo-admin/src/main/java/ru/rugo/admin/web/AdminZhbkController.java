package ru.rugo.admin.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.rugo.admin.stats.AdminZhbkStatsService;
import ru.rugo.admin.web.dto.AdminZhbkStatsDto;

/** Отдельная статистика ЖБК. Доступ — {@link AdminAuthFilter}. */
@RestController
@RequestMapping("/admin/api/zhbk")
@RequiredArgsConstructor
public class AdminZhbkController {

    private final AdminZhbkStatsService statsService;

    @GetMapping("/stats")
    public AdminZhbkStatsDto stats(@RequestParam(defaultValue = "7d") String period) {
        return statsService.stats(period);
    }
}
