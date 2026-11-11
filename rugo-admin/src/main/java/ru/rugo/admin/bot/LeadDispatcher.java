package ru.rugo.admin.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.rugo.admin.directory.AdminDirectory;
import ru.rugo.admin.notify.AdminMessenger;
import ru.rugo.core.entity.PartnerLead;
import ru.rugo.core.repository.PartnerLeadRepository;

import java.time.OffsetDateTime;
import java.util.List;

/** Находит новые заявки в БД и отправляет карточки админам (один раз). */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeadDispatcher {

    private final PartnerLeadRepository leadRepository;
    private final AdminMessenger messenger;
    private final AdminDirectory adminDirectory;

    @Value("${max.admin.bot.token:}")
    private String adminToken;

    /** Отправлять карточки заявок в чат админ-бота. По умолчанию выключено (работаем в веб-админке). */
    @Value("${admin.notify-cards-enabled:false}")
    private boolean notifyCardsEnabled;

    @Scheduled(fixedDelayString = "${admin.leads-scan-delay-ms:10000}", initialDelay = 5000)
    public void dispatch() {
        if (!notifyCardsEnabled) {
            return;
        }
        if (adminToken == null || adminToken.isBlank() || !adminDirectory.hasAdmins()) {
            return;
        }
        List<PartnerLead> leads = leadRepository
                .findByStatusAndNotifiedAtIsNullOrderByCreatedAtAsc(PartnerLead.STATUS_NEW);
        for (PartnerLead lead : leads) {
            try {
                // Помечаем заявку обработанной только при успешной доставке всем админам,
                // иначе пустой/неверный токен «съел» бы карточку навсегда.
                if (!messenger.sendCard(lead)) {
                    log.warn("Карточка заявки #{} не доставлена — повторим при следующем скане", lead.getId());
                    continue;
                }
                lead.setNotifiedAt(OffsetDateTime.now());
                leadRepository.save(lead);
                log.info("Карточка заявки #{} отправлена админам", lead.getId());
            } catch (Exception e) {
                log.error("Не удалось отправить карточку заявки id={}", lead.getId(), e);
            }
        }
    }
}
