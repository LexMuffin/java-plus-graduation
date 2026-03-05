package ru.yandex.practicum.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.dto.event.EventFullDto;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.dto.event.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByInitiator(Long userId, Pageable pageable);

    @Query("""
            SELECT e FROM Event e
            WHERE (COALESCE(:users) IS NULL OR e.initiator IN :users)
            AND (COALESCE(:states) IS NULL OR e.state IN :states)
            AND (COALESCE(:categories) IS NULL OR e.category IN :categories)
            AND (CAST(:rangeStart AS timestamp) IS NULL OR e.eventDate >= :rangeStart)
            AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.eventDate <= :rangeEnd)
            """)
    Page<Event> findAdminEvents(@Param("users") List<Long> users,
                                @Param("states") List<EventState> states,
                                @Param("categories") List<Long> categories,
                                @Param("rangeStart") LocalDateTime rangeStart,
                                @Param("rangeEnd") LocalDateTime rangeEnd,
                                Pageable pageable);

    @Query("""
            SELECT e FROM Event e
            WHERE e.state = 'PUBLISHED'
            AND (:text IS NULL OR :text = '' 
                 OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) 
                 OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%')))
            AND (:categories IS NULL OR e.category IN :categories)
            AND (:paid IS NULL OR e.paid = :paid)
            AND e.eventDate >= :rangeStart
            AND e.eventDate <= :rangeEnd
            """)
    Page<Event> findPublicEvents(@Param("text") String text,
                                 @Param("categories") List<Long> categories,
                                 @Param("paid") Boolean paid,
                                 @Param("rangeStart") LocalDateTime rangeStart,
                                 @Param("rangeEnd") LocalDateTime rangeEnd,
                                 Pageable pageable);

    boolean existsByCategory(Long categoryId);
}