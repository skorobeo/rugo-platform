package ru.rugo.core.service;

import ru.rugo.core.entity.Workshop;

import java.util.List;

public interface WorkshopService {

    Workshop save(Workshop workshop);

    List<Workshop> findAll();

    List<Workshop> findPending();

    List<Workshop> findApproved();

    Workshop findById(Long id);

    void delete(Long id);
}
