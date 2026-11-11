package ru.rugo.admin.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.rugo.admin.directory.AdminDirectory;
import ru.rugo.admin.security.AdminInitDataUserExtractor;
import ru.rugo.core.security.MaxInitDataValidator;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Доступ к {@code /admin/api/**} только по MAX id: проверяется подпись {@code X-Init-Data}
 * (токеном админ-бота, запас — токеном основного), а {@code user.id} должен быть в таблице {@code admins}.
 * Токена веб-админки нет.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthFilter extends OncePerRequestFilter {

    private final AdminDirectory adminDirectory;

    @Qualifier("adminInitDataValidator")
    private final MaxInitDataValidator adminInitDataValidator;

    @Qualifier("mainInitDataValidator")
    private final MaxInitDataValidator mainInitDataValidator;

    private final AdminInitDataUserExtractor userExtractor;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/admin/api/")) {
            chain.doFilter(request, response);
            return;
        }

        if (isAdminInitData(request)) {
            chain.doFilter(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private boolean isAdminInitData(HttpServletRequest request) {
        String initData = request.getHeader("X-Init-Data");

        Long byAdminToken = extractUserId(initData, adminInitDataValidator);
        if (adminDirectory.isAdmin(byAdminToken)) {
            return true;
        }
        Long byMainToken = extractUserId(initData, mainInitDataValidator);
        if (adminDirectory.isAdmin(byMainToken)) {
            return true;
        }

        log.warn("Доступ в админку отклонён: initDataPresent={}, userId(admin-токен)={}, userId(основной токен)={}",
                initData != null && !initData.isBlank(), byAdminToken, byMainToken);
        return false;
    }

    private Long extractUserId(String initData, MaxInitDataValidator validator) {
        Optional<Map<String, String>> params = validator.validate(initData);
        return params.map(userExtractor::extractUserId).orElse(null);
    }
}
