package ru.rugo.app.web.dto;

import ru.rugo.core.entity.PartnerProductPrice;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PartnerPriceDto(
        String variant,
        String temperatureBand,
        BigDecimal amount,
        String priceType,
        String currency,
        String unit,
        LocalDate validFrom,
        String note
) {

    public static PartnerPriceDto from(PartnerProductPrice price) {
        return new PartnerPriceDto(
                price.getVariant(),
                price.getTemperatureBand(),
                price.getAmount(),
                price.getPriceType(),
                price.getCurrency(),
                price.getUnit(),
                price.getValidFrom(),
                price.getNote()
        );
    }
}
