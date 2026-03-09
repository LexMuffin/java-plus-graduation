package ru.yandex.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.request.model.ParticipationRequest;
import ru.yandex.practicum.dto.request.RequestStatus;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequester(Long userId);

    List<ParticipationRequest> findByEvent(Long eventId);

    List<ParticipationRequest> findByEventAndIdIn(Long eventId, List<Long> requestIds);

    boolean existsByEventAndRequester(Long eventId, Long requesterId);

    int countByEventAndStatus(Long eventId, RequestStatus status);

    @Query("SELECT COUNT(r) FROM ParticipationRequest r WHERE r.event = :eventId AND r.status = 'CONFIRMED'")
    Long countConfirmedByEventId(@Param("eventId") Long eventId);

    @Query("SELECT r.event AS eventId, COUNT(r) AS count FROM ParticipationRequest r " +
            "WHERE r.event IN :eventIds AND r.status = 'CONFIRMED' " +
            "GROUP BY r.event")
    List<Object[]> countConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);
}