package ru.rugo.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.OffsetDateTime;

/** Настройки админ-модуля (префикс {@code admin} в application.yml). */
@ConfigurationProperties(prefix = "admin")
public record AdminProperties(
        /** Публичный базовый URL (для абсолютных ссылок на фото в карточке). */
        String publicBaseUrl,
        int pollTimeoutSeconds,
        int pollLimit,
        long leadsScanDelayMs,
        int logsTailLines,
        /** Начало статистики; всё, что раньше, в отчёты не попадает. */
        String statsStartAt
) {

    /** Точка старта статистики или {@code null}, если ограничения нет. */
    public OffsetDateTime statsStartAtOrNull() {
        if (statsStartAt == null || statsStartAt.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(statsStartAt.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Абсолютный URL фото из относительного пути {@code /uploads/...} или null. */
    public String absoluteUrl(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return null;
        }
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return base + (path.startsWith("/") ? path : "/" + path);
    }
}
