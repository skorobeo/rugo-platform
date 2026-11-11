package ru.rugo.app.web;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Внутренний сброс кэша каталога. Вызывается админкой (другой процесс) после правок,
 * чтобы изменения появлялись в мини-приложении мгновенно. Защищён токеном {@code INTERNAL_TOKEN};
 * наружу через Nginx не проксируется.
 */
@RestController
@RequestMapping("/internal/cache")
@RequiredArgsConstructor
public class InternalCacheController {

    private final CacheManager cacheManager;

    @Value("${app.internal-token:}")
    private String internalToken;

    @PostMapping("/evict")
    public ResponseEntity<Void> evict(@RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (internalToken == null || internalToken.isBlank() || token == null
                || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                        internalToken.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(401).build();
        }
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
        return ResponseEntity.noContent().build();
    }
}
