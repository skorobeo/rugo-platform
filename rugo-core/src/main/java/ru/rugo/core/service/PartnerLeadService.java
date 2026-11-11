package ru.rugo.core.service;

import ru.rugo.core.service.model.CreatePartnerLeadCommand;

public interface PartnerLeadService {

    /**
     * Валидирует и сохраняет заявку.
     *
     * @return идентификатор созданной заявки
     * @throws ru.rugo.common.exception.ApiValidationException  некорректные поля
     * @throws ru.rugo.common.exception.DuplicateLeadException  дубль телефона в окне дедупликации
     */
    Long createLead(CreatePartnerLeadCommand command);

    /**
     * Создаёт заявку из личного кабинета. Пользователь уже подтверждён через MAX,
     * поэтому антидубль по телефону не применяется: он блокировал бы добавление
     * второй карточки или материала сразу после первой.
     *
     * @return идентификатор созданной заявки
     * @throws ru.rugo.common.exception.ApiValidationException некорректные поля
     */
    Long createCabinetLead(CreatePartnerLeadCommand command);
}
