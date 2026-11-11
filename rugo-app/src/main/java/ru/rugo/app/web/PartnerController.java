package ru.rugo.app.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rugo.app.security.InitDataUserExtractor;
import ru.rugo.app.service.PartnerCabinetService;
import ru.rugo.app.service.PartnerEquipmentService;
import ru.rugo.app.web.dto.EquipmentDto;
import ru.rugo.app.web.dto.MaterialDto;
import ru.rugo.app.web.dto.PartnerEquipmentRequest;
import ru.rugo.app.web.dto.PartnerLeadStatusDto;
import ru.rugo.app.web.dto.PartnerMaterialChangeRequest;
import ru.rugo.app.web.dto.PartnerMaterialCreateRequest;
import ru.rugo.app.web.dto.PartnerProfileResponse;
import ru.rugo.app.web.dto.PhoneConfirmRequest;
import ru.rugo.common.config.AppProperties;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.common.exception.RateLimitExceededException;
import ru.rugo.common.exception.UnauthorizedInitDataException;
import ru.rugo.core.entity.User;
import ru.rugo.core.security.MaxInitDataValidator;
import ru.rugo.core.security.RequestContactValidator;
import ru.rugo.core.service.PartnerAccountService;
import ru.rugo.core.service.PartnerLeadService;
import ru.rugo.core.service.model.CreatePartnerLeadCommand;
import ru.rugo.core.util.PhoneNormalizer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Личный кабинет партнёра. Все методы требуют подписанный initData (MAX),
 * партнёр опознаётся по {@code max_user_id}.
 */
@RestController
@RequestMapping("/api/partner")
@RequiredArgsConstructor
public class PartnerController {

    private final MaxInitDataValidator initDataValidator;
    private final InitDataUserExtractor userExtractor;
    private final RequestContactValidator contactValidator;
    private final PartnerAccountService partnerAccountService;
    private final PartnerEquipmentService equipmentService;
    private final PartnerCabinetService cabinetService;
    private final PartnerLeadService partnerLeadService;
    private final FixedWindowRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final AppProperties properties;

    @GetMapping("/me")
    public PartnerProfileResponse me(
            @RequestHeader(value = "X-Init-Data", required = false) String initData) {
        Long maxUserId = requireUserId(initData);
        User user = partnerAccountService.findByMaxId(maxUserId);
        return profile(user);
    }

    @PostMapping("/phone")
    public PartnerProfileResponse confirmPhone(
            @Valid @RequestBody PhoneConfirmRequest request,
            @RequestHeader(value = "X-Init-Data", required = false) String initData,
            HttpServletRequest httpRequest) {

        Optional<Map<String, String>> params = initDataValidator.validate(initData);
        Long maxUserId = params.map(userExtractor::extractUserId).orElse(null);
        if (maxUserId == null) {
            throw new UnauthorizedInitDataException();
        }

        AppProperties.RateLimit limits = properties.rateLimit();
        if (!rateLimiter.tryAcquire("partner-phone:ip:" + clientIpResolver.resolve(httpRequest),
                limits.leadPerIp(), limits.leadIpWindowSeconds())
                || !rateLimiter.tryAcquire("partner-phone:user:" + maxUserId,
                limits.leadPerUser(), limits.leadUserWindowSeconds())) {
            throw new RateLimitExceededException();
        }

        String phone = PhoneNormalizer.normalize(request.phone()).orElse(null);
        if (phone == null) {
            throw new ApiValidationException(Map.of("phone", "Неверный номер"));
        }

        if (properties.contactSignatureRequired() && contactValidator.isEnabled()
                && !contactValidator.isValid(
                request.phone(), request.contactAuthDate(), request.contactHash(), maxUserId)) {
            throw new ApiValidationException(Map.of(
                    "phone", "Не удалось подтвердить номер. Получите номер из MAX заново."));
        }

        User user = partnerAccountService.resolveOrCreateForCabinet(
                maxUserId, phone, params.map(userExtractor::extractName).orElse(null));
        return profile(user);
    }

    @GetMapping("/equipment")
    public List<EquipmentDto> myEquipment(
            @RequestHeader(value = "X-Init-Data", required = false) String initData) {
        User owner = requirePartner(initData);
        return equipmentService.listOwned(owner.getId()).stream().map(EquipmentDto::from).toList();
    }

    @GetMapping("/materials")
    public List<MaterialDto> myMaterials(
            @RequestHeader(value = "X-Init-Data", required = false) String initData) {
        User owner = requirePartner(initData);
        return cabinetService.listMyMaterials(owner.getId()).stream().map(MaterialDto::from).toList();
    }

    @GetMapping("/leads")
    public List<PartnerLeadStatusDto> myLeads(
            @RequestHeader(value = "X-Init-Data", required = false) String initData) {
        Long maxUserId = requireUserId(initData);
        return cabinetService.listMyLeads(maxUserId).stream().map(PartnerLeadStatusDto::from).toList();
    }

    @PostMapping("/materials")
    public ResponseEntity<Map<String, Long>> createMaterial(
            @jakarta.validation.Valid @RequestBody PartnerMaterialCreateRequest request,
            @RequestHeader(value = "X-Init-Data", required = false) String initData) {
        User user = requirePartner(initData);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("category", request.category());
        if (request.fraction() != null) {
            details.put("fraction", request.fraction());
        }
        details.put("price", request.price());
        if (request.unit() != null) {
            details.put("unit", request.unit());
        }
        if (request.deliveryType() != null) {
            details.put("deliveryType", request.deliveryType());
        }
        if (request.photos() != null) {
            details.put("photos", request.photos());
        }
        Long id = partnerLeadService.createCabinetLead(new CreatePartnerLeadCommand(
                user.getMaxId(), safeName(user.getName()), user.getPhone(), List.of("materials"),
                effectivePartnerType(user, request.partnerType()),
                effectiveCompanyName(user, request.companyName()),
                user.getContactName(), request.district(), details, null, true));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @DeleteMapping("/materials/{id}")
    public ResponseEntity<Void> deleteMaterial(
            @PathVariable Long id,
            @RequestHeader(value = "X-Init-Data", required = false) String initData) {
        User owner = requirePartner(initData);
        cabinetService.deleteOwnedMaterial(owner.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/material-changes")
    public ResponseEntity<Map<String, Long>> materialChange(
            @jakarta.validation.Valid @RequestBody PartnerMaterialChangeRequest request,
            @RequestHeader(value = "X-Init-Data", required = false) String initData) {
        User user = requirePartner(initData);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("action", request.action());
        details.put("materialId", request.materialId());
        if (request.category() != null) {
            details.put("category", request.category());
        }
        if (request.fraction() != null) {
            details.put("fraction", request.fraction());
        }
        if (request.price() != null) {
            details.put("price", request.price());
        }
        if (request.unit() != null) {
            details.put("unit", request.unit());
        }
        if (request.deliveryType() != null) {
            details.put("deliveryType", request.deliveryType());
        }
        if (request.photos() != null) {
            details.put("photos", request.photos());
        }
        String name = user.getName() != null && !user.getName().isBlank() ? user.getName() : "Партнёр";
        Long id = partnerLeadService.createCabinetLead(new CreatePartnerLeadCommand(
                user.getMaxId(), name, user.getPhone(), List.of("materials"),
                effectivePartnerType(user, null), effectiveCompanyName(user, null),
                user.getContactName(), request.district(), details, null, true));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @PostMapping("/equipment")
    public ResponseEntity<Map<String, Long>> createEquipment(
            @Valid @RequestBody PartnerEquipmentRequest request,
            @RequestHeader(value = "X-Init-Data", required = false) String initData) {
        User user = requirePartner(initData);
        Long id = partnerLeadService.createCabinetLead(new CreatePartnerLeadCommand(
                user.getMaxId(), safeName(user.getName()), user.getPhone(), List.of("equipment"),
                effectivePartnerType(user, request.partnerType()),
                effectiveCompanyName(user, request.companyName()),
                user.getContactName(), request.district(), equipmentDetails(request, null), null, true));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @PutMapping("/equipment/{id}")
    public ResponseEntity<Map<String, Long>> updateEquipment(
            @PathVariable Long id,
            @Valid @RequestBody PartnerEquipmentRequest request,
            @RequestHeader(value = "X-Init-Data", required = false) String initData) {
        User user = requirePartner(initData);
        Long leadId = partnerLeadService.createCabinetLead(new CreatePartnerLeadCommand(
                user.getMaxId(), safeName(user.getName()), user.getPhone(), List.of("equipment"),
                effectivePartnerType(user, request.partnerType()),
                effectiveCompanyName(user, request.companyName()),
                user.getContactName(), request.district(), equipmentDetails(request, id), null, true));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", leadId));
    }

    @DeleteMapping("/equipment/{id}")
    public ResponseEntity<Void> deleteEquipment(
            @PathVariable Long id,
            @RequestHeader(value = "X-Init-Data", required = false) String initData) {
        User owner = requirePartner(initData);
        equipmentService.delete(owner, id);
        return ResponseEntity.noContent().build();
    }

    /** Детали заявки по технике. При {@code equipmentId != null} — заявка на изменение существующей карточки. */
    private Map<String, Object> equipmentDetails(PartnerEquipmentRequest request, Long equipmentId) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (equipmentId != null) {
            details.put("action", "update");
            details.put("equipmentId", equipmentId);
        }
        details.put("category", request.category());
        if (request.brand() != null) {
            details.put("brand", request.brand());
        }
        if (request.model() != null) {
            details.put("model", request.model());
        }
        details.put("capacity", request.capacity());
        if (request.boomLength() != null) {
            details.put("boomLength", request.boomLength());
        }
        if (request.description() != null) {
            details.put("description", request.description());
        }
        details.put("price", request.price());
        if (request.priceUnit() != null) {
            details.put("priceUnit", request.priceUnit());
        }
        if (request.imageUrls() != null) {
            details.put("photos", request.imageUrls());
        }
        return details;
    }

    /**
     * Кабинет не должен понижать компанию до частного лица. Если пользователь уже
     * компания, берём сохранённый тип и название; иначе используем выбор из формы.
     */
    private String effectivePartnerType(User user, String requested) {
        if ("company".equals(user.getPartnerType())) {
            return "company";
        }
        return requested == null || requested.isBlank() ? user.getPartnerType() : requested;
    }

    private String effectiveCompanyName(User user, String requested) {
        if ("company".equals(user.getPartnerType())) {
            return user.getName();
        }
        return requested == null || requested.isBlank() ? null : requested;
    }

    /** Имя из MAX может не пройти строгий шаблон — нормализуем и подстраховываемся. */
    private String safeName(String raw) {
        if (raw == null) {
            return "Партнёр";
        }
        String s = raw.replaceAll("[^A-Za-zА-Яа-яЁё \\-]", "").trim();
        if (s.length() < 2) {
            return "Партнёр";
        }
        return s.length() > 60 ? s.substring(0, 60) : s;
    }

    private User requirePartner(String initData) {
        Long maxUserId = requireUserId(initData);
        User user = partnerAccountService.findByMaxId(maxUserId);
        if (user == null) {
            throw new ApiValidationException(Map.of(
                    "profile", "Сначала подтвердите номер телефона в личном кабинете"));
        }
        return user;
    }

    private Long requireUserId(String initData) {
        Optional<Map<String, String>> params = initDataValidator.validate(initData);
        Long maxUserId = params.map(userExtractor::extractUserId).orElse(null);
        if (maxUserId == null) {
            throw new UnauthorizedInitDataException();
        }
        return maxUserId;
    }

    private PartnerProfileResponse profile(User user) {
        if (user == null) {
            return new PartnerProfileResponse(false, null, null, null, null);
        }
        return new PartnerProfileResponse(
                true, user.getId(), user.getName(), user.getPhone(), user.getPartnerType());
    }
}
