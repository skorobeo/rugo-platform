package ru.rugo.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.rugo.core.entity.Material;
import ru.rugo.core.repository.MaterialRepository;
import ru.rugo.core.service.MaterialService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository repository;

    @Override
    @Cacheable(value = "materials", key = "#type")
    public List<Material> findByTypeWithSupplier(String type) {
        return repository.findByTypeWithSupplier(type);
    }

    @Override
    @Cacheable(value = "materials", key = "#type + '_avail'")
    public List<Material> findApprovedAvailableByTypeWithSupplier(String type) {
        return repository.findByTypeApprovedAvailableWithSupplier(type);
    }

    @Override
    public List<Material> findPending() {
        return repository.findByStatusOrderByCreatedAtAsc(Material.STATUS_PENDING);
    }

    @Override
    public List<Material> findByType(String type) {
        return repository.findByTypeContainingIgnoreCase(type);
    }

    @Override
    public List<Material> findAvailableByType(String type) {
        return repository.findByTypeContainingIgnoreCaseAndAvailableTrue(type);
    }

    @Override
    public List<Material> findAllAvailable() {
        return repository.findByAvailableTrueOrderByPricePerUnitAsc();
    }

    @Override
    public List<Material> findBySupplier(Long supplierId) {
        return repository.findBySupplierId(supplierId);
    }

    @Override
    public List<Material> findBySupplierPhone(String phone) {
        return repository.findBySupplierPhoneWithSupplier(phone);
    }

    @Override
    public Material findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    @CacheEvict(value = "materials", allEntries = true)
    public Material save(Material material) {
        return repository.save(material);
    }

    @Override
    @CacheEvict(value = "materials", allEntries = true)
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
