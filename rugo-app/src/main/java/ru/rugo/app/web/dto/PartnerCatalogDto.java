package ru.rugo.app.web.dto;

import java.util.List;

public record PartnerCatalogDto(
        PartnerInfoDto info,
        List<PartnerGroupDto> groups,
        long totalProducts
) {
}
