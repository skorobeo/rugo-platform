package ru.rugo.app.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Извлекает {@code user.id} из разобранных параметров проверенного initData.
 * Значение {@code user} — JSON-строка (см. docs/miniapp/MAX-DOCS-CHECK.md §1.4).
 * Не является доказательством личности сам по себе: используется только после
 * успешной проверки подписи.
 */
@Component
@RequiredArgsConstructor
public class InitDataUserExtractor {

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

    /** Отображаемое имя пользователя MAX ({@code first_name [last_name]}), если есть. */
    public String extractName(Map<String, String> params) {
        if (params == null) {
            return null;
        }
        String user = params.get("user");
        if (user == null || user.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(user);
            String first = text(node, "first_name");
            String last = text(node, "last_name");
            StringBuilder sb = new StringBuilder();
            if (first != null) {
                sb.append(first);
            }
            if (last != null) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(last);
            }
            return sb.length() == 0 ? null : sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText(null);
    }
}
