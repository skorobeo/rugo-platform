package ru.rugo.app.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rugo.common.config.AppProperties;

import java.util.List;

/**
 * Определяет клиентский IP с учётом обратного прокси (Nginx).
 *
 * <p>Заголовкам {@code X-Forwarded-For}/{@code X-Real-IP} доверяем <b>только</b> если
 * непосредственный пир ({@code getRemoteAddr()}) входит в список доверенных прокси
 * ({@code app.trusted-proxies}, по умолчанию loopback). Для {@code X-Forwarded-For}
 * берём <b>ближайший к прокси</b> (последний добавленный доверенным прокси) адрес,
 * который сам не является доверенным прокси — так клиент не может подделать IP,
 * дописав свой адрес в начало заголовка (V-029).
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private final AppProperties properties;

    public String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (!isTrusted(remote)) {
            return remote;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] hops = forwardedFor.split(",");
            for (int i = hops.length - 1; i >= 0; i--) {
                String candidate = hops[i].trim();
                if (!candidate.isEmpty() && !isTrusted(candidate)) {
                    return candidate;
                }
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return remote;
    }

    private boolean isTrusted(String ip) {
        if (ip == null) {
            return false;
        }
        List<String> trusted = properties.trustedProxies();
        return trusted != null && trusted.contains(ip);
    }
}
