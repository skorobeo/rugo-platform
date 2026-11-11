package ru.rugo.app.web.dto;

/**
 * Профиль партнёра для личного кабинета. {@code registered=false} — партнёр ещё не
 * подтвердил номер (кабинет в этом случае просит поделиться телефоном).
 */
public record PartnerProfileResponse(
        boolean registered,
        Long id,
        String name,
        String phone,
        String partnerType
) {
}
