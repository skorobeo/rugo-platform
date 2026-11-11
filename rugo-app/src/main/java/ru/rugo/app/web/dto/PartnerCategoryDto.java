package ru.rugo.app.web.dto;

import ru.rugo.core.entity.PartnerCategory;

public record PartnerCategoryDto(
        String slug,
        String title,
        String imageUrl,
        long productCount
) {

    public static PartnerCategoryDto from(PartnerCategory category, long productCount) {
        return new PartnerCategoryDto(
                category.getSlug(),
                category.getTitle(),
                category.getImageUrl(),
                productCount
        );
    }
}
