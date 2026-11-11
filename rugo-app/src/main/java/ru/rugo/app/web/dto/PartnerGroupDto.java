package ru.rugo.app.web.dto;

import java.util.List;

public record PartnerGroupDto(
        String slug,
        String title,
        List<PartnerCategoryDto> categories
) {
}
