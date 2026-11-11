package ru.rugo.core.service;

import ru.rugo.core.entity.Equipment;
import ru.rugo.core.enums.EquipmentStatus;

import java.util.List;

public interface EquipmentService {

    List<Equipment> findByCategoryWithOwner(String category, String location);

    List<Equipment> findAvailableByCategoryWithOwner(String category, String location);

    List<Equipment> findPending();

    List<Equipment> findByCategory(String category, String location);

    List<Equipment> findAvailableByCategory(String category, String location);

    List<Equipment> findAllAvailable();

    List<Equipment> findByOwner(Long ownerId);

    List<Equipment> findByOwnerPhone(String phone);

    Equipment findById(Long id);

    Equipment save(Equipment equipment);

    void delete(Long id);
}
