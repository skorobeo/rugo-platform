package ru.rugo.admin.notify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.model.AttachmentRequest;
import ru.max.botapi.model.CallbackAnswer;
import ru.max.botapi.model.NewMessageBody;
import ru.rugo.admin.bot.LeadCardBuilder;
import ru.rugo.admin.config.AdminProperties;
import ru.rugo.admin.directory.AdminDirectory;
import ru.rugo.core.entity.PartnerLead;

import java.util.ArrayList;
import java.util.List;

/** Отправка сообщений админ-ботом (карточки, ответы) и основным ботом (партнёру). */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminMessenger {

    @Qualifier("adminBotApi")
    private final MaxBotAPI adminBotApi;

    @Qualifier("mainBotApi")
    private final MaxBotAPI mainBotApi;

    private final AdminProperties properties;
    private final AdminDirectory adminDirectory;

    /** Шлёт карточку заявки всем админам. */
    public boolean sendCard(PartnerLead lead) {
        List<AttachmentRequest> attachments = new ArrayList<>(LeadCardBuilder.photoAttachments(lead, properties));
        attachments.add(LeadCardBuilder.keyboard(lead.getId()));
        NewMessageBody body = new NewMessageBody(LeadCardBuilder.text(lead), attachments, null, null, null);

        boolean delivered = true;
        for (Long adminId : adminDirectory.userIds()) {
            delivered &= send(adminBotApi, adminId, body);
        }
        return delivered;
    }

    /** Текстовое сообщение админу (ответы на команды). */
    public void sendText(long chatId, String text) {
        send(adminBotApi, chatId, new NewMessageBody(text, null, null, null, null));
    }

    public void answerCallback(String callbackId, String notification) {
        try {
            adminBotApi.answerOnCallback(new CallbackAnswer(null, notification), callbackId).execute();
        } catch (Exception e) {
            log.warn("Не удалось ответить на callback: {}", e.toString());
        }
    }

    /** Уведомление партнёру — только основным ботом. */
    public void notifyPartner(Long maxUserId, String text) {
        if (maxUserId == null) {
            return;
        }
        try {
            mainBotApi.sendMessage(new NewMessageBody(text, null, null, null, null))
                    .userId(maxUserId)
                    .execute();
        } catch (Exception e) {
            log.warn("Не удалось уведомить партнёра {}: {}", maxUserId, e.toString());
        }
    }

    /**
     * Отправляет сообщение пользователю синхронно ({@code execute()}), чтобы поймать и залогировать
     * ошибки MAX. Для личных сообщений нужен именно {@code user_id} (с {@code chat_id} MAX отдаёт
     * {@code 404 Chat not found}).
     * @return {@code true} при успехе; {@code false} — при ошибке (тогда заявку нужно переотправить).
     */
    private boolean send(MaxBotAPI api, long userId, NewMessageBody body) {
        try {
            api.sendMessage(body).userId(userId).execute();
            return true;
        } catch (Exception e) {
            log.error("Не удалось отправить сообщение пользователю {}: {}", userId, e.toString());
            return false;
        }
    }
}
