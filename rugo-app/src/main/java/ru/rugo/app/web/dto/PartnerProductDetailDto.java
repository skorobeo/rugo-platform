package ru.rugo.app.web.dto;

import java.util.List;

public record PartnerProductDetailDto(
        PartnerProductDto product,
        List<PartnerProductDto> related
) {
}
