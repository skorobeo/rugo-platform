package ru.rugo.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rugo.core.entity.Workshop;
import ru.rugo.core.repository.WorkshopRepository;
import ru.rugo.core.service.WorkshopService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkshopServiceImpl implements WorkshopService {

    private final WorkshopRepository repository;

    @Override
    public Workshop save(Workshop workshop) {
        return repository.save(workshop);
    }

    @Override
    public List<Workshop> findAll() {
        return repository.findAll();
    }

    @Override
    public List<Workshop> findPending() {
        return repository.findByStatusOrderByCreatedAtAsc(Workshop.STATUS_PENDING);
    }

    @Override
    public List<Workshop> findApproved() {
        return repository.findByStatusOrderByCreatedAtAsc(Workshop.STATUS_APPROVED);
    }

    @Override
    public Workshop findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
