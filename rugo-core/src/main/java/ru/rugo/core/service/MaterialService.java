package ru.rugo.core.service;

import ru.rugo.core.entity.Material;

import java.util.List;

public interface MaterialService {

    List<Material> findByTypeWithSupplier(String type);

    List<Material> findApprovedAvailableByTypeWithSupplier(String type);

    List<Material> findPending();

    List<Material> findByType(String type);

    List<Material> findAvailableByType(String type);

    List<Material> findAllAvailable();

    List<Material> findBySupplier(Long supplierId);

    List<Material> findBySupplierPhone(String phone);

    Material findById(Long id);

    Material save(Material material);

    void delete(Long id);
}
