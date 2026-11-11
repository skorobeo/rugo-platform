package ru.rugo.admin.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.rugo.admin.config.AdminProperties;
import ru.rugo.admin.notify.AdminMessenger;
import ru.rugo.admin.stats.AdminStatsService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Stream;

/** Текстовые команды админ-бота. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminCommands {

    private static final String HELP = """
            Команды:
            /stats — статистика (заходы, заявки, реферальные ссылки)
            /logs — последние строки лога
            """;

    private final AdminMessenger messenger;
    private final AdminProperties properties;
    private final AdminStatsService statsService;

    public void handle(long chatId, String text) {
        if (text == null) {
            return;
        }
        String message = text.trim();
        if (message.equals("/start") || message.equals("/help")) {
            messenger.sendText(chatId, HELP);
            return;
        }
        if (message.equals("/logs")) {
            messenger.sendText(chatId, tailLog());
            return;
        }
        if (message.equals("/stats")) {
            messenger.sendText(chatId, statsService.summary());
        }
    }

    String tailLog() {
        // LOG_DIR может задаваться и как system property, и как переменная окружения (как в logback).
        String logDir = System.getProperty("LOG_DIR", System.getenv().getOrDefault("LOG_DIR", "logs"));
        Path path = Paths.get(logDir, "rugo-admin.log");
        if (!Files.exists(path)) {
            return "Файл лога не найден: " + path.toAbsolutePath();
        }
        int limit = Math.max(1, properties.logsTailLines());
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            Deque<String> tail = new ArrayDeque<>();
            lines.forEach(line -> {
                tail.addLast(line);
                if (tail.size() > limit) {
                    tail.removeFirst();
                }
            });
            if (tail.isEmpty()) {
                return "(лог пуст)";
            }
            return String.join("\n", List.copyOf(tail));
        } catch (Exception e) {
            log.warn("Не удалось прочитать лог: {}", e.toString());
            return "Не удалось прочитать лог: " + e.getMessage();
        }
    }
}
