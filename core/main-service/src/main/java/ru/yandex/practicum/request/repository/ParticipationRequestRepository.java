package ru.yandex.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.request.model.ParticipationRequest;
import ru.yandex.practicum.request.model.RequestStatus;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequesterId(Long userId);

    List<ParticipationRequest> findByEventId(Long eventId);

    List<ParticipationRequest> findByEventIdAndIdIn(Long eventId, List<Long> requestIds);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    int countByEventIdAndStatus(Long eventId, RequestStatus status);
}
