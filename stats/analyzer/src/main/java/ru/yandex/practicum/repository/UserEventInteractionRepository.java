package ru.yandex.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.model.UserEventInteraction;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserEventInteractionRepository extends JpaRepository<UserEventInteraction, Long> {

    Optional<UserEventInteraction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserEventInteraction> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    @Query("SELECT uei.eventId, SUM(uei.weight) FROM UserEventInteraction uei " +
            "WHERE uei.eventId IN :eventIds GROUP BY uei.eventId")
    List<Object[]> sumWeightsByEventIds(@Param("eventIds") List<Long> eventIds);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    List<UserEventInteraction> findByUserIdAndEventIdIn(Long userId, List<Long> eventIds);
}
