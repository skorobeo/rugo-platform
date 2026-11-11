package ru.rugo.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rugo.common.exception.EntityNotFoundException;
import ru.rugo.core.entity.Equipment;
import ru.rugo.core.entity.User;
import ru.rugo.core.enums.EquipmentStatus;
import ru.rugo.core.repository.EquipmentRepository;

import java.util.List;

/**
 * Чтение и удаление своей техники из личного кабинета. Создание и правка идут
 * через заявочный контур ({@code PartnerLead}) — карточка появляется после одобрения.
 */
@Service
@RequiredArgsConstructor
public class PartnerEquipmentService {

    private final EquipmentRepository equipmentRepository;

    @Transactional(readOnly = true)
    public List<Equipment> listOwned(Long ownerId) {
        return equipmentRepository.findByOwnerIdAndStatusNotOrderByCreatedAtDesc(
                ownerId, EquipmentStatus.HIDDEN);
    }

    @Transactional(readOnly = true)
    public Equipment getOwned(Long ownerId, Long id) {
        return equipmentRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Техника #" + id + " не найдена"));
    }

    @Transactional
    @CacheEvict(value = "equipment", allEntries = true)
    public void delete(User owner, Long id) {
        Equipment target = getOwned(owner.getId(), id);
        equipmentRepository.delete(target);
    }
}
