package ru.rugo.app.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.rugo.core.storage.PhotoStorageService;
import ru.rugo.app.web.dto.UploadResponse;
import ru.rugo.common.config.AppProperties;
import ru.rugo.common.exception.RateLimitExceededException;
import ru.rugo.common.exception.UnauthorizedInitDataException;
import ru.rugo.core.security.MaxInitDataValidator;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final MaxInitDataValidator initDataValidator;
    private final PhotoStorageService photoStorageService;
    private final FixedWindowRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final AppProperties properties;

    @PostMapping
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Init-Data", required = false) String initData,
            HttpServletRequest httpRequest) {

        if (!properties.photoUploadsEnabled()) {
            return ResponseEntity.notFound().build();
        }

        Optional<Map<String, String>> initDataParams = initDataValidator.validate(initData);
        if (initDataParams.isEmpty() && !properties.allowAnonLeads()) {
            throw new UnauthorizedInitDataException();
        }

        AppProperties.RateLimit limits = properties.rateLimit();
        if (!rateLimiter.tryAcquire("upload:" + clientIpResolver.resolve(httpRequest),
                limits.leadPerIp(), limits.leadIpWindowSeconds())) {
            throw new RateLimitExceededException();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UploadResponse(photoStorageService.store(file)));
    }
}
