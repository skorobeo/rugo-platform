package ru.rugo.admin.catalog;

import ru.rugo.admin.events.AfterCommit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.core.util.TextRules;
import ru.rugo.admin.web.dto.AdminWorkshopDto;
import ru.rugo.admin.web.dto.AdminWorkshopRequest;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.common.exception.EntityNotFoundException;
import ru.rugo.core.entity.Workshop;
import ru.rugo.core.repository.WorkshopRepository;
import ru.rugo.core.util.CatalogOptions;
import ru.rugo.core.util.PhoneNormalizer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CRUD мастерских из админки — поля как у партнёра (компания, специализация, город, район, телефон). */
@Service
@RequiredArgsConstructor
public class AdminWorkshopService {

    private static final int MAX_NAME = 150;
    private static final int MAX_SPECIALIZATION = 500;

    private final WorkshopRepository workshopRepository;
    private final CatalogCacheEvictor cacheEvictor;

    @Transactional(readOnly = true)
    public List<AdminWorkshopDto> list(String status) {
        String s = trim(status);
        String effective = s == null
                ? Workshop.STATUS_APPROVED
                : ("all".equalsIgnoreCase(s) ? null : s);
        return workshopRepository.findForAdmin(effective).stream()
                .map(AdminWorkshopDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminWorkshopDto get(Long id) {
        return AdminWorkshopDto.from(require(id));
    }

    @Transactional
    public Long create(AdminWorkshopRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        Workshop parsed = parse(request, errors);
        if (!errors.isEmpty()) {
            throw new ApiValidationException(errors);
        }
        parsed.setConsent(true);
        Long id = workshopRepository.save(parsed).getId();
        AfterCommit.run(cacheEvictor::evict);
        return id;
    }

    @Transactional
    public Long update(Long id, AdminWorkshopRequest request) {
        Workshop target = require(id);
        Map<String, String> errors = new LinkedHashMap<>();
        Workshop parsed = parse(request, errors);
        if (!errors.isEmpty()) {
            throw new ApiValidationException(errors);
        }
        copy(parsed, target);
        workshopRepository.save(target);
        AfterCommit.run(cacheEvictor::evict);
        return target.getId();
    }

    @Transactional
    public void delete(Long id) {
        if (!workshopRepository.existsById(id)) {
            throw new EntityNotFoundException("Мастерская #" + id + " не найдена");
        }
        workshopRepository.deleteById(id);
        AfterCommit.run(cacheEvictor::evict);
    }

    private Workshop parse(AdminWorkshopRequest request, Map<String, String> errors) {
        Workshop w = new Workshop();

        String name = trim(request.name());
        if (name == null) {
            errors.put("name", "Укажите название компании");
        } else if (name.length() > MAX_NAME || !TextRules.PLAIN.matcher(name).matches()) {
            errors.put("name", "Название: " + TextRules.PLAIN_HINT + " (до " + MAX_NAME + " символов)");
        }
        w.setName(name);

        String specialization = trim(request.specialization());
        if (specialization == null) {
            errors.put("specialization", "Укажите специализацию");
        } else if (specialization.length() > MAX_SPECIALIZATION
                || !TextRules.DESCRIPTION.matcher(specialization).matches()) {
            errors.put("specialization",
                    "Специализация: " + TextRules.DESCRIPTION_HINT + " (до " + MAX_SPECIALIZATION + " символов)");
        }
        w.setSpecialization(specialization);

        String city = trim(request.city());
        if (city == null) {
            city = "Воронеж";
        } else if (!CatalogOptions.CITIES.contains(city)) {
            errors.put("city", "Выберите город из списка");
        }

        String district = trim(request.district());
        if (district != null && !CatalogOptions.DISTRICTS.contains(district)) {
            errors.put("district", "Выберите район из списка");
        }
        w.setLocation(district == null ? city : city + ", " + district);

        String phone = PhoneNormalizer.normalize(request.phone()).orElse(null);
        if (phone == null) {
            errors.put("phone", "Укажите корректный телефон");
        }
        w.setPhone(phone);

        w.setStatus(Workshop.STATUS_APPROVED);
        return w;
    }

    private void copy(Workshop from, Workshop to) {
        to.setName(from.getName());
        to.setSpecialization(from.getSpecialization());
        to.setLocation(from.getLocation());
        to.setPhone(from.getPhone());
    }

    private Workshop require(Long id) {
        return workshopRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Мастерская #" + id + " не найдена"));
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
