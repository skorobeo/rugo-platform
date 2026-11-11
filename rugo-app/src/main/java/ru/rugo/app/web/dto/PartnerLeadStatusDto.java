package ru.rugo.app.web.dto;

import ru.rugo.core.entity.PartnerLead;

import java.time.OffsetDateTime;
import java.util.List;

/** Краткая карточка заявки партнёра для раздела «На проверке» в личном кабинете. */
public record PartnerLeadStatusDto(
        Long id,
        List<String> offerTypes,
        String status,
        OffsetDateTime createdAt
) {

    public static PartnerLeadStatusDto from(PartnerLead lead) {
        return new PartnerLeadStatusDto(
                lead.getId(),
                lead.getOfferTypes(),
                lead.getStatus(),
                lead.getCreatedAt());
    }
}
