package ru.yandex.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.model.EventSimilarity;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    Optional<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);

    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA = :eventId OR es.eventB = :eventId")
    List<EventSimilarity> findAllByEventId(@Param("eventId") Long eventId);

    @Query("SELECT es FROM EventSimilarity es " +
            "WHERE (es.eventA = :eventId OR es.eventB = :eventId) " +
            "AND es.score > 0 ORDER BY es.score DESC")
    List<EventSimilarity> findSimilarEvents(@Param("eventId") Long eventId, Pageable pageable);

    @Query("SELECT es FROM EventSimilarity es WHERE " +
            "(es.eventA = :targetEvent AND es.eventB IN :candidateEvents) OR " +
            "(es.eventB = :targetEvent AND es.eventA IN :candidateEvents)")
    List<EventSimilarity> findSimilaritiesBetween(
            @Param("targetEvent") Long targetEvent,
            @Param("candidateEvents") List<Long> candidateEvents);

    @Query("SELECT es.score FROM EventSimilarity es WHERE " +
            "(es.eventA = :eventA AND es.eventB = :eventB) OR " +
            "(es.eventA = :eventB AND es.eventB = :eventA)")
    Optional<Double> findSimilarityBetween(@Param("eventA") Long eventA,
                                           @Param("eventB") Long eventB);

    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA IN :eventIds OR es.eventB IN :eventIds")
    List<EventSimilarity> findAllByEventIds(@Param("eventIds") List<Long> eventIds);
}
