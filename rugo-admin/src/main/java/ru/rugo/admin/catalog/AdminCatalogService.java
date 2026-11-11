package ru.rugo.admin.catalog;

import ru.rugo.admin.events.AfterCommit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.admin.web.dto.AdminEquipmentDto;
import ru.rugo.admin.web.dto.AdminEquipmentRequest;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.common.exception.EntityNotFoundException;
import ru.rugo.core.entity.Equipment;
import ru.rugo.core.entity.User;
import ru.rugo.core.enums.EquipmentStatus;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.repository.UserRepository;
import ru.rugo.core.service.EquipmentAssembler;
import ru.rugo.core.util.PhoneNormalizer;
import ru.rugo.core.util.TextRules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CRUD техники из админки. Поля и правила — как в партнёрской форме:
 * категория, марка, модель, грузоподъёмность (тонн), длина стрелы (для манипулятора),
 * цена + единица, район, описание, фото. Карточки привязываются к системному владельцу.
 */
@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    static final String SYSTEM_OWNER_PHONE = "+70000000000";
    static final String SYSTEM_OWNER_NAME = "RuGo (админ)";

    /** Статусы, которые считаются опубликованными (показываются в каталоге по умолчанию). */
    private static final List<EquipmentStatus> PUBLISHED_STATUSES = List.of(
            EquipmentStatus.AVAILABLE,
            EquipmentStatus.BUSY,
            EquipmentStatus.RESERVED,
            EquipmentStatus.BOOKED);

    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final CatalogCacheEvictor cacheEvictor;

    @Transactional(readOnly = true)
    public List<AdminEquipmentDto> list(String category, String status) {
        String c = trim(category);
        String s = trim(status);
        if (s == null) {
            return equipmentRepository.findForAdminByStatuses(c, PUBLISHED_STATUSES).stream()
                    .map(AdminEquipmentDto::from)
                    .toList();
        }
        if ("all".equalsIgnoreCase(s)) {
            return equipmentRepository.findForAdmin(c, null).stream()
                    .map(AdminEquipmentDto::from)
                    .toList();
        }
        EquipmentStatus parsedStatus = parseStatus(s);
        if (parsedStatus == null) {
            return List.of();
        }
        return equipmentRepository.findForAdmin(c, parsedStatus).stream()
                .map(AdminEquipmentDto::from)
                .toList();
    }

    private EquipmentStatus parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return EquipmentStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public AdminEquipmentDto get(Long id) {
        return AdminEquipmentDto.from(require(id));
    }

    @Transactional
    public Long addEquipment(AdminEquipmentRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        Equipment parsed = parse(request, errors);
        if (!errors.isEmpty()) {
            throw new ApiValidationException(errors);
        }
        if (parsed.getOwner() == null) {
            parsed.setOwner(systemOwner());
        }
        parsed.setConsent(true);
        Long id = equipmentRepository.save(parsed).getId();
        AfterCommit.run(cacheEvictor::evict);
        return id;
    }

    @Transactional
    public Long updateEquipment(Long id, AdminEquipmentRequest request) {
        Equipment target = require(id);
        Map<String, String> errors = new LinkedHashMap<>();
        Equipment parsed = parse(request, errors);
        if (!errors.isEmpty()) {
            throw new ApiValidationException(errors);
        }
        copy(parsed, target);
        if (request.imageUrls() != null) {
            target.setImageUrl(parsed.getImageUrl());
            target.setImageUrls(parsed.getImageUrls());
        }
        equipmentRepository.save(target);
        AfterCommit.run(cacheEvictor::evict);
        return target.getId();
    }

    @Transactional
    public void delete(Long id) {
        if (!equipmentRepository.existsById(id)) {
            throw new EntityNotFoundException("Техника #" + id + " не найдена");
        }
        equipmentRepository.deleteById(id);
        AfterCommit.run(cacheEvictor::evict);
    }

    /** Публикует карточку после проверки. */
    @Transactional
    public String approve(Long id) {
        Equipment equipment = require(id);
        equipment.setStatus(EquipmentStatus.AVAILABLE);
        equipmentRepository.save(equipment);
        AfterCommit.run(cacheEvictor::evict);
        return "одобрено";
    }

    /** Отклоняет карточку: скрывает из каталога, не удаляя. */
    @Transactional
    public String reject(Long id) {
        Equipment equipment = require(id);
        equipment.setStatus(EquipmentStatus.HIDDEN);
        equipmentRepository.save(equipment);
        AfterCommit.run(cacheEvictor::evict);
        return "отклонено";
    }

    private Equipment parse(AdminEquipmentRequest request, Map<String, String> errors) {
        Equipment e = EquipmentAssembler.assemble(new EquipmentAssembler.Input(
                request.category(),
                request.brand(),
                request.model(),
                request.capacity(),
                request.boomLength(),
                request.price(),
                request.priceUnit(),
                request.district(),
                request.description(),
                request.imageUrls()), errors);
        e.setYear(request.year());
        e.setStatus(EquipmentStatus.AVAILABLE);

        String ownerName = optionalText(request.ownerName(), "ownerName", "Имя владельца", 100, errors);
        String ownerPhone = trim(request.ownerPhone());
        if (ownerPhone != null) {
            String normalized = PhoneNormalizer.normalize(ownerPhone).orElse(null);
            if (normalized == null) {
                errors.put("ownerPhone", "Укажите корректный телефон владельца");
            } else {
                e.setOwner(resolveOwner(normalized, ownerName));
            }
        }
        return e;
    }

    private void copy(Equipment from, Equipment to) {
        to.setCategory(from.getCategory());
        to.setType(from.getType());
        to.setBrand(from.getBrand());
        to.setModel(from.getModel());
        to.setCapacity(from.getCapacity());
        to.setBoomLength(from.getBoomLength());
        to.setPrice(from.getPrice());
        to.setPriceType(from.getPriceType());
        to.setLocation(from.getLocation());
        to.setYear(from.getYear());
        to.setDescription(from.getDescription());
        if (from.getOwner() != null) {
            to.setOwner(from.getOwner());
        }
    }

    private Equipment require(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Техника #" + id + " не найдена"));
    }

    private String optionalText(String raw, String field, String label, int max, Map<String, String> errors) {
        String value = trim(raw);
        if (value == null) {
            return null;
        }
        if (value.length() > max || !TextRules.PLAIN.matcher(value).matches()) {
            errors.put(field, label + ": " + TextRules.PLAIN_HINT + " (до " + max + " символов)");
        }
        return value;
    }

    private User resolveOwner(String phone, String name) {
        return userRepository.findByPhone(phone).orElseGet(() ->
                userRepository.save(User.builder()
                        .phone(phone)
                        .name(name == null ? "Партнёр" : name)
                        .role(User.ROLE_PROVIDER)
                        .status(User.STATUS_ACTIVE)
                        .build()));
    }

    private User systemOwner() {
        return userRepository.findByPhone(SYSTEM_OWNER_PHONE).orElseGet(() ->
                userRepository.save(User.builder()
                        .phone(SYSTEM_OWNER_PHONE)
                        .name(SYSTEM_OWNER_NAME)
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
