package ru.rugo.admin.web.dto;

import ru.rugo.core.entity.PartnerLead;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Карточка заявки для веб-админки. */
public record AdminLeadDto(
        Long id,
        String name,
        String phone,
        List<String> offerTypes,
        String partnerType,
        String companyName,
        String contactName,
        String district,
        String source,
        Map<String, Object> details,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime decidedAt
) {

    public static AdminLeadDto from(PartnerLead lead) {
        return new AdminLeadDto(
                lead.getId(),
                lead.getName(),
                lead.getPhone(),
                lead.getOfferTypes(),
                lead.getPartnerType(),
                lead.getCompanyName(),
                lead.getContactName(),
                lead.getDistrict(),
                lead.getSource(),
                lead.getDetails(),
                lead.getStatus(),
                lead.getCreatedAt(),
                lead.getDecidedAt()
        );
    }
}
