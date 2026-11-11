package ru.rugo.app.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rugo.app.security.InitDataUserExtractor;
import ru.rugo.app.web.dto.LeadDetails;
import ru.rugo.app.web.dto.PartnerLeadRequest;
import ru.rugo.app.web.dto.PartnerLeadResponse;
import ru.rugo.common.config.AppProperties;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.common.exception.RateLimitExceededException;
import ru.rugo.common.exception.UnauthorizedInitDataException;
import ru.rugo.core.security.MaxInitDataValidator;
import ru.rugo.core.security.RequestContactValidator;
import ru.rugo.core.service.PartnerLeadService;
import ru.rugo.core.service.model.CreatePartnerLeadCommand;
import ru.rugo.core.util.SourceCode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/partner-leads")
@RequiredArgsConstructor
public class PartnerLeadController {

    private final PartnerLeadService partnerLeadService;
    private final MaxInitDataValidator initDataValidator;
    private final InitDataUserExtractor userExtractor;
    private final RequestContactValidator contactValidator;
    private final FixedWindowRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final AppProperties properties;

    @PostMapping
    public ResponseEntity<PartnerLeadResponse> create(
            @Valid @RequestBody PartnerLeadRequest request,
            @RequestHeader(value = "X-Init-Data", required = false) String initData,
            HttpServletRequest httpRequest) {

        Optional<Map<String, String>> initDataParams = initDataValidator.validate(initData);
        if (initDataParams.isEmpty() && !properties.allowAnonLeads()) {
            throw new UnauthorizedInitDataException();
        }

        Long maxUserId = initDataParams.map(userExtractor::extractUserId).orElse(null);
        String startParam = initDataParams.map(params -> params.get("start_param")).orElse(null);

        checkRateLimit(clientIpResolver.resolve(httpRequest), maxUserId);

        if (isHoneypotFilled(request)) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        enforceContactSignature(request, maxUserId);

        String source = SourceCode.sanitize(startParam != null ? startParam : request.source());
        LeadDetails details = request.details();

        CreatePartnerLeadCommand command = new CreatePartnerLeadCommand(
                maxUserId,
                request.name(),
                request.phone(),
                request.offerTypes() == null ? List.of() : request.offerTypes(),
                request.partnerType(),
                request.companyName(),
                request.contactName(),
                request.district(),
                details == null ? Map.of() : details.toMap(),
                source,
                Boolean.TRUE.equals(request.consent())
        );

        Long id = partnerLeadService.createLead(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(new PartnerLeadResponse(id));
    }

    private void checkRateLimit(String ip, Long maxUserId) {
        AppProperties.RateLimit limits = properties.rateLimit();
        if (!rateLimiter.tryAcquire("ip:" + ip, limits.leadPerIp(), limits.leadIpWindowSeconds())) {
            throw new RateLimitExceededException();
        }
        if (maxUserId != null
                && !rateLimiter.tryAcquire("user:" + maxUserId, limits.leadPerUser(), limits.leadUserWindowSeconds())) {
            throw new RateLimitExceededException();
        }
    }

    private boolean isHoneypotFilled(PartnerLeadRequest request) {
        return request.website() != null && !request.website().isBlank();
    }

    /**
     * Проверяет подпись номера, полученного через {@code requestContact}.
     * Выполняется только когда есть токен бота и подтверждённый пользователь
     * из initData (в dev-профиле и при анонимных заявках — пропускается).
     */
    private void enforceContactSignature(PartnerLeadRequest request, Long maxUserId) {
        if (!properties.contactSignatureRequired() || !contactValidator.isEnabled() || maxUserId == null) {
            return;
        }
        if (!contactValidator.isValid(
                request.phone(), request.contactAuthDate(), request.contactHash(), maxUserId)) {
            throw new ApiValidationException(
                    Map.of("phone", "Не удалось подтвердить номер. Получите номер из MAX заново."));
        }
    }
}
