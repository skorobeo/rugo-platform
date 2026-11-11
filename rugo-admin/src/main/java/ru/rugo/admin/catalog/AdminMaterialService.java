package ru.rugo.admin.catalog;

import ru.rugo.admin.events.AfterCommit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.core.util.TextRules;
import ru.rugo.admin.web.dto.AdminMaterialDto;
import ru.rugo.admin.web.dto.AdminMaterialRequest;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.common.exception.EntityNotFoundException;
import ru.rugo.core.entity.Material;
import ru.rugo.core.entity.User;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.repository.UserRepository;
import ru.rugo.core.service.MaterialAssembler;
import ru.rugo.core.util.PhoneNormalizer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CRUD материалов из админки — поля как у партнёра (тип, фракция, цена, единица, получение, район). */
@Service
@RequiredArgsConstructor
public class AdminMaterialService {

    private static final int MAX_NAME = 100;

    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final CatalogCacheEvictor cacheEvictor;

    @Transactional(readOnly = true)
    public List<AdminMaterialDto> list(String type, String status) {
        String t = trim(type);
        String s = trim(status);
        String effective = s == null
                ? Material.STATUS_APPROVED
                : ("all".equalsIgnoreCase(s) ? null : s);
        return materialRepository.findForAdmin(t, effective).stream()
                .map(AdminMaterialDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminMaterialDto get(Long id) {
        return AdminMaterialDto.from(require(id));
    }

    @Transactional
    public Long create(AdminMaterialRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        Material parsed = parse(request, errors);
        if (!errors.isEmpty()) {
            throw new ApiValidationException(errors);
        }
        if (parsed.getSupplier() == null) {
            parsed.setSupplier(systemSupplier());
        }
        parsed.setConsent(true);
        Long id = materialRepository.save(parsed).getId();
        AfterCommit.run(cacheEvictor::evict);
        return id;
    }

    @Transactional
    public Long update(Long id, AdminMaterialRequest request) {
        Material target = require(id);
        Map<String, String> errors = new LinkedHashMap<>();
        Material parsed = parse(request, errors);
        if (!errors.isEmpty()) {
            throw new ApiValidationException(errors);
        }
        copy(parsed, target);
        if (request.imageUrls() != null) {
            target.setImageUrl(parsed.getImageUrl());
            target.setImageUrls(parsed.getImageUrls());
        }
        materialRepository.save(target);
        AfterCommit.run(cacheEvictor::evict);
        return target.getId();
    }

    @Transactional
    public void delete(Long id) {
        if (!materialRepository.existsById(id)) {
            throw new EntityNotFoundException("Материал #" + id + " не найден");
        }
        materialRepository.deleteById(id);
        AfterCommit.run(cacheEvictor::evict);
    }

    /** Публикация или скрытие материала из каталога. */
    @Transactional
    public String setStatus(Long id, String status) {
        String value = trim(status);
        if (!Material.STATUS_APPROVED.equals(value) && !Material.STATUS_HIDDEN.equals(value)) {
            throw new ApiValidationException(Map.of("status", "Допустимые статусы: APPROVED, HIDDEN"));
        }
        Material material = require(id);
        material.setStatus(value);
        materialRepository.save(material);
        AfterCommit.run(cacheEvictor::evict);
        return value;
    }

    private Material parse(AdminMaterialRequest request, Map<String, String> errors) {
        Material m = MaterialAssembler.assemble(new MaterialAssembler.Input(
                request.category(),
                request.fraction(),
                request.price(),
                request.unit(),
                request.deliveryType(),
                request.district(),
                request.imageUrls()), errors);
        m.setStatus(Material.STATUS_APPROVED);

        String supplierName = safe(request.supplierName(), "supplierName", "Имя поставщика", MAX_NAME, errors);
        String supplierPhone = trim(request.supplierPhone());
        if (supplierPhone != null) {
            String normalized = PhoneNormalizer.normalize(supplierPhone).orElse(null);
            if (normalized == null) {
                errors.put("supplierPhone", "Укажите корректный телефон поставщика");
            } else {
                m.setSupplier(resolveSupplier(normalized, supplierName));
            }
        }
        return m;
    }

    private void copy(Material from, Material to) {
        to.setType(from.getType());
        to.setFraction(from.getFraction());
        to.setPricePerUnit(from.getPricePerUnit());
        to.setUnit(from.getUnit());
        to.setDeliveryType(from.getDeliveryType());
        to.setLocation(from.getLocation());
        to.setAvailable(from.getAvailable());
        // imageUrl намеренно не копируем: в запросе редактирования фото нет,
        // иначе существующее изображение затиралось бы (V-018).
        if (from.getSupplier() != null) {
            to.setSupplier(from.getSupplier());
        }
    }

    private Material require(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Материал #" + id + " не найден"));
    }

    private String safe(String raw, String field, String label, int max, Map<String, String> errors) {
        String value = trim(raw);
        if (value == null) {
            return null;
        }
        if (value.length() > max || !TextRules.PLAIN.matcher(value).matches()) {
            errors.put(field, label + ": " + TextRules.PLAIN_HINT + " (до " + max + " символов)");
        }
        return value;
    }

    private User resolveSupplier(String phone, String name) {
        return userRepository.findByPhone(phone).orElseGet(() ->
                userRepository.save(User.builder()
                        .phone(phone)
                        .name(name == null ? "Партнёр" : name)
                        .role(User.ROLE_PROVIDER)
                        .status(User.STATUS_ACTIVE)
                        .build()));
    }

    private User systemSupplier() {
        return userRepository.findByPhone(AdminCatalogService.SYSTEM_OWNER_PHONE).orElseGet(() ->
                userRepository.save(User.builder()
                        .phone(AdminCatalogService.SYSTEM_OWNER_PHONE)
                        .name(AdminCatalogService.SYSTEM_OWNER_NAME)
                        .role(User.ROLE_PROVIDER)
                        .status(User.STATUS_ACTIVE)
                        .build()));
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
