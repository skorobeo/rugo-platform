package ru.rugo.admin.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.rugo.admin.moderation.ModerationService;
import ru.rugo.admin.stats.AdminStatsService;
import ru.rugo.admin.web.dto.AdminLeadDto;
import ru.rugo.admin.web.dto.AdminStatsDto;
import ru.rugo.core.entity.PartnerLead;
import ru.rugo.core.repository.PartnerLeadRepository;

import java.util.List;
import java.util.Map;

/** REST-API веб-админки. Доступ проверяется {@link AdminAuthFilter}. */
@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class AdminWebController {

    private final PartnerLeadRepository leadRepository;
    private final ModerationService moderationService;
    private final AdminStatsService statsService;

    @GetMapping("/leads")
    public List<AdminLeadDto> leads(@RequestParam(defaultValue = PartnerLead.STATUS_NEW) String status) {
        List<PartnerLead> leads = ("all".equalsIgnoreCase(status) || status.isBlank())
                ? leadRepository.findAllByOrderByCreatedAtDesc()
                : leadRepository.findByStatusOrderByCreatedAtDesc(status);
        return leads.stream().map(AdminLeadDto::from).toList();
    }

    @PostMapping("/leads/{id}/approve")
    public Map<String, String> approve(@PathVariable Long id) {
        return Map.of("result", moderationService.approve(id));
    }

    @PostMapping("/leads/{id}/reject")
    public Map<String, String> reject(@PathVariable Long id) {
        return Map.of("result", moderationService.reject(id));
    }

    @PostMapping("/leads/{id}/reopen")
    public Map<String, String> reopen(@PathVariable Long id) {
        return Map.of("result", moderationService.reopen(id));
    }

    @GetMapping("/leads/{id}/contact")
    public Map<String, String> contact(@PathVariable Long id) {
        return Map.of("text", moderationService.contactInfo(id));
    }

    @GetMapping("/stats")
    public AdminStatsDto stats() {
        return statsService.stats();
    }
}
