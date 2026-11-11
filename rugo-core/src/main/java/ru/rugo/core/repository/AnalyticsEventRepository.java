package ru.rugo.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rugo.core.entity.AnalyticsEvent;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    long countByEvent(String event);

    @Query("SELECT e.event, COUNT(e) FROM AnalyticsEvent e GROUP BY e.event ORDER BY COUNT(e) DESC")
    List<Object[]> countsByEvent();

    @Query("SELECT COUNT(DISTINCT e.userHash) FROM AnalyticsEvent e")
    long countDistinctUsers();

    @Query("SELECT e.event, COUNT(e), COUNT(DISTINCT e.userHash) FROM AnalyticsEvent e GROUP BY e.event ORDER BY COUNT(e) DESC")
    List<Object[]> countsByEventWithUnique();

    List<AnalyticsEvent> findTop200ByOrderByCreatedAtDesc();

    List<AnalyticsEvent> findTop200ByEventOrderByCreatedAtDesc(String event);

    List<AnalyticsEvent> findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(OffsetDateTime from);

    // --- Те же агрегаты, но с исключением служебных пользователей (админов) ---

    @Query("SELECT COUNT(e) FROM AnalyticsEvent e WHERE e.event = :event AND e.userHash NOT IN :excluded")
    long countByEventExcluding(@Param("event") String event, @Param("excluded") Collection<String> excluded);

    @Query("SELECT e.event, COUNT(e) FROM AnalyticsEvent e WHERE e.userHash NOT IN :excluded "
            + "GROUP BY e.event ORDER BY COUNT(e) DESC")
    List<Object[]> countsByEventExcluding(@Param("excluded") Collection<String> excluded);

    @Query("SELECT COUNT(DISTINCT e.userHash) FROM AnalyticsEvent e WHERE e.userHash NOT IN :excluded")
    long countDistinctUsersExcluding(@Param("excluded") Collection<String> excluded);

    @Query("SELECT e.event, COUNT(e), COUNT(DISTINCT e.userHash) FROM AnalyticsEvent e "
            + "WHERE e.userHash NOT IN :excluded GROUP BY e.event ORDER BY COUNT(e) DESC")
    List<Object[]> countsByEventWithUniqueExcluding(@Param("excluded") Collection<String> excluded);
}
