package ru.rugo.admin.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.max.botapi.model.Callback;
import ru.max.botapi.model.Message;
import ru.max.botapi.model.MessageCallbackUpdate;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.Update;
import ru.rugo.admin.directory.AdminDirectory;
import ru.rugo.admin.moderation.ModerationService;
import ru.rugo.admin.notify.AdminMessenger;

/** Разбор апдейтов админ-бота: нажатия кнопок и текстовые команды. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUpdateHandler {

    private final AdminMessenger messenger;
    private final ModerationService moderationService;
    private final AdminDirectory adminDirectory;
    private final AdminCommands commands;

    public void handle(Update update) {
        if (update instanceof MessageCallbackUpdate callback) {
            handleCallback(callback);
        } else if (update instanceof MessageCreatedUpdate created) {
            handleMessage(created);
        }
    }

    private void handleCallback(MessageCallbackUpdate update) {
        Callback callback = update.callback();
        long userId = callback.user().userId();
        if (!isAdmin(userId)) {
            messenger.answerCallback(callback.callbackId(), "Нет доступа");
            return;
        }
        String payload = callback.payload();
        if (payload == null || !payload.startsWith("lead:")) {
            messenger.answerCallback(callback.callbackId(), "Неизвестное действие");
            return;
        }
        String[] parts = payload.split(":");
        if (parts.length != 3) {
            messenger.answerCallback(callback.callbackId(), "Некорректные данные");
            return;
        }
        Long leadId;
        try {
            leadId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            messenger.answerCallback(callback.callbackId(), "Некорректный id заявки");
            return;
        }
        String action = parts[2];

        switch (action) {
            case LeadCardBuilder.ACTION_APPROVE -> {
                String result = moderationService.approve(leadId);
                messenger.answerCallback(callback.callbackId(), "Одобрено");
                messenger.sendText(userId, "✅ Заявка #" + leadId + ": " + result);
            }
            case LeadCardBuilder.ACTION_REJECT -> {
                String result = moderationService.reject(leadId);
                messenger.answerCallback(callback.callbackId(), "Отклонено");
                messenger.sendText(userId, "❌ Заявка #" + leadId + ": " + result);
            }
            case LeadCardBuilder.ACTION_CONTACT -> {
                String info = moderationService.contactInfo(leadId);
                messenger.answerCallback(callback.callbackId(), "Контакт отправлен");
                messenger.sendText(userId, info);
            }
            default -> messenger.answerCallback(callback.callbackId(), "Неизвестное действие");
        }
    }

    private void handleMessage(MessageCreatedUpdate update) {
        Message message = update.message();
        if (message.sender() == null) {
            return;
        }
        long userId = message.sender().userId();
        if (!isAdmin(userId)) {
            return;
        }
        commands.handle(userId, message.body().text());
    }

    private boolean isAdmin(long userId) {
        return adminDirectory.isAdmin(userId);
    }
}
