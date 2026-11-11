package ru.rugo.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.core.entity.User;
import ru.rugo.core.repository.UserRepository;

import java.util.Map;

/**
 * Единая точка получения/создания партнёра ({@link User}) из заявки или кабинета.
 * Используется и админкой (одобрение заявки), и мини-приложением (личный кабинет),
 * чтобы правила привязки по {@code max_id}/{@code phone} не расходились.
 */
@Service
@RequiredArgsConstructor
public class PartnerAccountService {

    private final UserRepository userRepository;

    /** Ищет партнёра по id в MAX. */
    @Transactional(readOnly = true)
    public User findByMaxId(Long maxId) {
        return maxId == null ? null : userRepository.findByMaxId(maxId).orElse(null);
    }

    /**
     * Сценарий личного кабинета: находит партнёра по {@code maxId} (затем по телефону)
     * или создаёт нового. Если телефон уже привязан к другому аккаунту MAX — отказ:
     * самостоятельно перепривязать кабинет нельзя, только через поддержку.
     */
    @Transactional
    public User resolveOrCreateForCabinet(Long maxId, String phone, String name) {
        User byMaxId = maxId == null ? null : userRepository.findByMaxId(maxId).orElse(null);
        if (byMaxId != null) {
            return byMaxId;
        }

        User byPhone = phone == null || phone.isBlank()
                ? null
                : userRepository.findByPhone(phone).orElse(null);
        if (byPhone != null && byPhone.getMaxId() != null && !byPhone.getMaxId().equals(maxId)) {
            throw new ApiValidationException(Map.of(
                    "phone", "Этот номер уже привязан к другому аккаунту MAX. Обратитесь в поддержку"));
        }

        if (byPhone != null) {
            if (maxId != null && byPhone.getMaxId() == null) {
                byPhone.setMaxId(maxId);
            }
            if (name != null && !name.isBlank()
                    && (byPhone.getName() == null || byPhone.getName().isBlank())) {
                byPhone.setName(name.trim());
            }
            if (byPhone.getRole() == null || byPhone.getRole().isBlank()) {
                byPhone.setRole(User.ROLE_PROVIDER);
            }
            return userRepository.save(byPhone);
        }

        String displayName = name == null || name.isBlank() ? "Партнёр" : name.trim();
        return userRepository.save(User.builder()
                .phone(phone == null ? "" : phone)
                .name(displayName)
                .role(User.ROLE_PROVIDER)
                .status(User.STATUS_ACTIVE)
                .maxId(maxId)
                .build());
    }

    /**
     * Находит партнёра по {@code maxId} (затем по телефону) или создаёт нового.
     * Обновляет недостающие/изменившиеся поля. Телефон всегда обязателен для нового партнёра.
     */
    @Transactional
    public User resolveOrCreate(
            Long maxId,
            String phone,
            String name,
            String partnerType,
            String contactName) {
        User user = maxId == null ? null : userRepository.findByMaxId(maxId).orElse(null);
        if (user == null && phone != null && !phone.isBlank()) {
            user = userRepository.findByPhone(phone).orElse(null);
        }

        String displayName = name == null || name.isBlank() ? "Партнёр" : name.trim();

        if (user == null) {
            return userRepository.save(User.builder()
                    .phone(phone == null ? "" : phone)
                    .name(displayName)
                    .role(User.ROLE_PROVIDER)
                    .status(User.STATUS_ACTIVE)
                    .partnerType(partnerType)
                    .contactName(contactName)
                    .maxId(maxId)
                    .build());
        }

        if (maxId != null && user.getMaxId() == null) {
            user.setMaxId(maxId);
        }
        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone);
        }
        if ("company".equals(partnerType)) {
            // Одобренная компания-заявка повышает существующий кабинет до компании.
            // Название компании храним в name, контактное лицо — отдельно.
            user.setPartnerType("company");
            user.setName(displayName);
            if (contactName != null && !contactName.isBlank()) {
                user.setContactName(contactName.trim());
            }
        } else if (partnerType != null && !partnerType.isBlank()
                && (user.getPartnerType() == null || user.getPartnerType().isBlank())) {
            user.setPartnerType(partnerType);
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(displayName);
        }
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole(User.ROLE_PROVIDER);
        }
        return userRepository.save(user);
    }
}
