package ru.rugo.app.web.dto;

import ru.rugo.core.entity.PartnerCatalog;

import java.util.List;
import java.util.Map;

public record PartnerInfoDto(
        String name,
        String subtitle,
        String phone,
        String logoUrl,
        List<PartnerFactDto> facts,
        List<String> clients
) {

    public static PartnerInfoDto from(PartnerCatalog catalog) {
        List<PartnerFactDto> facts = catalog.getFacts() == null ? List.of() : catalog.getFacts().stream()
                .map(item -> new PartnerFactDto(text(item, "value"), text(item, "label")))
                .toList();
        return new PartnerInfoDto(
                catalog.getName(),
                catalog.getSubtitle(),
                catalog.getPhone(),
                catalog.getLogoUrl(),
                facts,
                catalog.getClients() == null ? List.of() : catalog.getClients()
        );
    }

    private static String text(Map<String, String> item, String key) {
        if (item == null) {
            return null;
        }
        Object value = item.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
