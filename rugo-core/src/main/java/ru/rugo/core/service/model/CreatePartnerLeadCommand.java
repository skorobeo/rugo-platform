package ru.rugo.core.service.model;

import java.util.List;
import java.util.Map;

/**
 * Команда создания заявки партнёра. Телефон — «сырой», нормализуется в сервисе.
 * {@code maxUserId} берётся только из проверенного initData (может быть null при allow-anon).
 */
public record CreatePartnerLeadCommand(
        Long maxUserId,
        String name,
        String phone,
        List<String> offerTypes,
        String partnerType,
        String companyName,
        String contactName,
        String district,
        Map<String, Object> details,
        String source,
        boolean consent
) {
}
