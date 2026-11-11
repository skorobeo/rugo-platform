package ru.rugo.admin.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PartnerCatalogSeed(
        String version,
        PartnerSeed partner,
        List<GroupSeed> groups,
        List<ProductSeed> products
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PartnerSeed(
            String name,
            String subtitle,
            String phone,
            String logoUrl,
            List<Map<String, String>> facts,
            List<String> clients
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroupSeed(
            String slug,
            String title,
            Integer sortOrder,
            List<CategorySeed> categories
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CategorySeed(
            String slug,
            String title,
            String imageUrl,
            Integer sortOrder
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductSeed(
            String externalKey,
            String sourceUrl,
            String groupId,
            String categorySlug,
            String name,
            String unit,
            String availability,
            String tu,
            BigDecimal weightT,
            BigDecimal lengthM,
            BigDecimal widthM,
            BigDecimal thicknessM,
            BigDecimal volumeM3,
            String imageUrl,
            List<Map<String, String>> attributes,
            Integer sortOrder,
            Boolean active,
            List<PriceSeed> prices
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PriceSeed(
            String variant,
            String temperatureBand,
            String priceType,
            BigDecimal amount,
            String currency,
            String unit,
            LocalDate validFrom,
            LocalDate validTo,
            String note
    ) {
    }
}
