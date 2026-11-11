package ru.rugo.admin.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Извлекает {@code user.id} из разобранных параметров проверенного initData
 * админ-мини-приложения ({@code user} — JSON-строка). Вызывать только после
 * успешной проверки подписи {@link ru.rugo.core.security.MaxInitDataValidator}.
 */
@Component
@RequiredArgsConstructor
public class AdminInitDataUserExtractor {

    private final ObjectMapper objectMapper;

    public Long extractUserId(Map<String, String> params) {
        if (params == null) {
            return null;
        }
        String user = params.get("user");
        if (user == null || user.isBlank()) {
            return null;
        }
        try {
            JsonNode id = objectMapper.readTree(user).get("id");
            return id == null || !id.canConvertToLong() ? null : id.asLong();
        } catch (Exception e) {
            return null;
        }
    }
}
