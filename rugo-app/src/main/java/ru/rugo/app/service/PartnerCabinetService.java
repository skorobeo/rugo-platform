package ru.rugo.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.common.exception.ApiValidationException;
import ru.rugo.common.exception.EntityNotFoundException;
import ru.rugo.core.entity.Material;
import ru.rugo.core.entity.PartnerLead;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.repository.PartnerLeadRepository;

import java.util.List;
import java.util.Map;

/** Чтение/изменение данных партнёра для личного кабинета: свои материалы и заявки. */
@Service
@RequiredArgsConstructor
public class PartnerCabinetService {

    private final MaterialRepository materialRepository;
    private final PartnerLeadRepository leadRepository;

    /** Материалы партнёра (кроме скрытых). */
    @Transactional(readOnly = true)
    public List<Material> listMyMaterials(Long ownerId) {
        return materialRepository.findOwnedWithSupplier(ownerId, Material.STATUS_HIDDEN);
    }

    /** Заявки партнёра (по MAX id), новые сверху. */
    @Transactional(readOnly = true)
    public List<PartnerLead> listMyLeads(Long maxUserId) {
        return leadRepository.findByMaxUserIdOrderByCreatedAtDesc(maxUserId);
    }

    /** Удаление своего материала (окончательно, без модерации). */
    @Transactional
    @CacheEvict(value = "materials", allEntries = true)
    public void deleteOwnedMaterial(Long ownerId, Long materialId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new EntityNotFoundException("Материал #" + materialId + " не найден"));
        if (material.getSupplier() == null || !ownerId.equals(material.getSupplier().getId())) {
            throw new ApiValidationException(Map.of("materialId", "Это не ваш материал"));
        }
        materialRepository.delete(material);
    }
}
