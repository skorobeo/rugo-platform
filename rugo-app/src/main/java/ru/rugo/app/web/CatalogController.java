package ru.rugo.app.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.rugo.app.web.dto.CategoryCountDto;
import ru.rugo.app.web.dto.EquipmentDto;
import ru.rugo.app.web.dto.MaterialDto;
import ru.rugo.app.web.dto.WorkshopDto;
import ru.rugo.core.enums.EquipmentStatus;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.service.EquipmentService;
import ru.rugo.core.service.MaterialService;
import ru.rugo.core.service.WorkshopService;

import java.util.List;

/**
 * Публичное чтение каталога для мини-приложения: техника, материалы, мастерские.
 * Только чтение, без initData. Отдаём DTO (см. web/dto/*Dto).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private static final String DEFAULT_LOCATION = "Воронеж";
    private static final List<EquipmentStatus> VISIBLE_STATUSES = List.of(
            EquipmentStatus.AVAILABLE, EquipmentStatus.BUSY, EquipmentStatus.RESERVED, EquipmentStatus.BOOKED);

    private final EquipmentService equipmentService;
    private final MaterialService materialService;
    private final WorkshopService workshopService;
    private final EquipmentRepository equipmentRepository;
    private final MaterialRepository materialRepository;

    @GetMapping("/equipment")
    public List<EquipmentDto> equipment(
            @RequestParam String category,
            @RequestParam(defaultValue = DEFAULT_LOCATION) String location) {
        return equipmentService.findAvailableByCategoryWithOwner(category, location).stream()
                .map(EquipmentDto::from)
                .toList();
    }

    /** Категории техники с количеством доступных карточек, по убыванию (для сортировки списка). */
    @GetMapping("/equipment/categories")
    public List<CategoryCountDto> equipmentCategories(
            @RequestParam(defaultValue = DEFAULT_LOCATION) String location) {
        return equipmentRepository.countsByCategory(VISIBLE_STATUSES, location).stream()
                .map(row -> new CategoryCountDto((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    /** Другие предложения того же партнёра (по телефону владельца). */
    @GetMapping("/equipment/related")
    public List<EquipmentDto> relatedEquipment(@RequestParam String phone) {
        return equipmentRepository.findByOwnerPhoneAndStatusesWithOwner(phone, VISIBLE_STATUSES).stream()
                .map(EquipmentDto::from)
                .toList();
    }

    @GetMapping("/materials")
    public List<MaterialDto> materials(@RequestParam String type) {
        return materialService.findApprovedAvailableByTypeWithSupplier(type).stream()
                .map(MaterialDto::from)
                .toList();
    }

    /** Типы материалов с количеством доступных предложений, по убыванию. */
    @GetMapping("/materials/types")
    public List<CategoryCountDto> materialTypes() {
        return materialRepository.countsByType().stream()
                .map(row -> new CategoryCountDto((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    @GetMapping("/workshops")
    public List<WorkshopDto> workshops() {
        return workshopService.findApproved().stream()
                .map(WorkshopDto::from)
                .toList();
    }
}
