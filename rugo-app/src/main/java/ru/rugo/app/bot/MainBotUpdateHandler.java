package ru.rugo.app.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.model.BotStartedUpdate;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.NewMessageBody;
import ru.max.botapi.model.Update;

/**
 * Минимальная чат-логика основного бота: только приветствие.
 * Отвечает на «Начать» ({@code bot_started}) и на команду {@code /start}.
 * Остальные сообщения игнорируются — диалогов нет.
 */
@Slf4j
@Component
public class MainBotUpdateHandler {

    private final MaxBotAPI api;
    private final String greeting;

    public MainBotUpdateHandler(
            MaxBotAPI api,
            @Value("${app.bot-greeting:}") String greeting) {
        this.api = api;
        this.greeting = greeting;
    }

    public void handle(Update update) {
        if (update instanceof BotStartedUpdate started) {
            sendGreeting(started.chatId());
        } else if (update instanceof MessageCreatedUpdate created) {
            String text = created.message().body().text();
            if (text != null && text.trim().startsWith("/start")) {
                sendGreeting(created.message().recipient().chatId());
            }
        }
    }

    private void sendGreeting(Long chatId) {
        if (chatId == null || greeting == null || greeting.isBlank()) {
            return;
        }
        try {
            api.sendMessage(new NewMessageBody(greeting, null, null, null, null))
                    .chatId(chatId)
                    .enqueue();
        } catch (Exception e) {
            log.warn("Не удалось отправить приветствие в чат {}: {}", chatId, e.toString());
        }
    }
}
