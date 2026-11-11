package ru.rugo.app.web.dto;

import java.util.List;

public record PartnerProductPageDto(
        PartnerCategoryDto category,
        List<PartnerProductDto> items,
        long total,
        int page,
        int size
) {
}
