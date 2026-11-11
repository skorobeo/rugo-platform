package ru.rugo.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.rugo.core.entity.Equipment;
import ru.rugo.core.enums.EquipmentStatus;
import ru.rugo.core.repository.EquipmentRepository;
import ru.rugo.core.service.EquipmentService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository repository;

    private static final List<EquipmentStatus> VISIBLE_STATUSES = List.of(
            EquipmentStatus.AVAILABLE,
            EquipmentStatus.BUSY,
            EquipmentStatus.RESERVED,
            EquipmentStatus.BOOKED
    );

    @Override
    @Cacheable(value = "equipment", key = "#category + '_' + #location")
    public List<Equipment> findByCategoryWithOwner(String category, String location) {
        return repository.findByCategoryWithOwner(
                category,
                location != null ? location : "Воронеж"
        );
    }

    @Override
    @Cacheable(value = "equipment", key = "#category + '_' + #location + '_avail'")
    public List<Equipment> findAvailableByCategoryWithOwner(String category, String location) {
        return repository.findByCategoryAndStatusesWithOwner(
                category,
                VISIBLE_STATUSES,
                location != null ? location : "Воронеж"
        );
    }

    @Override
    public List<Equipment> findPending() {
        return repository.findByStatusOrderByCreatedAtAsc(EquipmentStatus.PENDING);
    }

    @Override
    public List<Equipment> findByCategory(String category, String location) {
        return repository.findByCategoryAndLocationContainingIgnoreCase(
                category,
                location != null ? location : "Воронеж"
        );
    }

    @Override
    public List<Equipment> findAvailableByCategory(String category, String location) {
        return repository.findByCategoryAndStatusAndLocationContainingIgnoreCase(
                category,
                EquipmentStatus.AVAILABLE,
                location != null ? location : "Воронеж"
        );
    }

    @Override
    public List<Equipment> findAllAvailable() {
        return repository.findByStatusOrderByReliabilityDescPriceAsc(EquipmentStatus.AVAILABLE);
    }

    @Override
    public List<Equipment> findByOwner(Long ownerId) {
        return repository.findByOwnerId(ownerId);
    }

    @Override
    public List<Equipment> findByOwnerPhone(String phone) {
        return repository.findByOwnerPhoneWithOwner(phone);
    }

    @Override
    public Equipment findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    @CacheEvict(value = "equipment", allEntries = true)
    public Equipment save(Equipment equipment) {
        return repository.save(equipment);
    }

    @Override
    @CacheEvict(value = "equipment", allEntries = true)
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
