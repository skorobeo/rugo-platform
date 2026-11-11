package ru.rugo.admin.catalog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Сбрасывает кэш каталога в rugo-app (отдельный процесс) после правок из админки,
 * чтобы изменения появлялись в мини-приложении мгновенно. Если {@code INTERNAL_TOKEN}
 * не задан — ничего не делает (остаётся TTL кэша).
 */
@Slf4j
@Service
public class CatalogCacheEvictor {

    private final RestClient restClient;
    private final String baseUrl;
    private final String token;

    public CatalogCacheEvictor(
            @Value("${app.internal-base-url:http://127.0.0.1:8080}") String baseUrl,
            @Value("${app.internal-token:}") String token) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.restClient = RestClient.create();
    }

    public void evict() {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            restClient.post()
                    .uri(baseUrl + "/internal/cache/evict")
                    .header("X-Internal-Token", token)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Не удалось сбросить кэш каталога: {}", e.toString());
        }
    }
}
