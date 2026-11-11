package ru.rugo.admin.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Выполняет действие ПОСЛЕ коммита текущей транзакции (V-017). Внешние вызовы
 * (сообщение в MAX, сброс кэша по HTTP) не должны удерживаться внутри транзакции:
 * долгий внешний вызов продлевает транзакцию, а его сбой мог бы откатить бизнес-операцию.
 *
 * <p>Если активной транзакции нет (например, в юнит-тестах без Spring-контекста),
 * действие выполняется сразу. Исключения действия логируются и не пробрасываются.
 */
@Slf4j
public final class AfterCommit {

    private AfterCommit() {
    }

    public static void run(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runSafely(action);
                }
            });
        } else {
            runSafely(action);
        }
    }

    private static void runSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.warn("Отложенное действие после коммита не выполнено: {}", e.toString());
        }
    }
}
