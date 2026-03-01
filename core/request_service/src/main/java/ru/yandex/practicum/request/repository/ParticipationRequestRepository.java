package ru.yandex.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.request.model.ParticipationRequest;
import ru.yandex.practicum.dto.request.RequestStatus;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequester(Long userId);

    List<ParticipationRequest> findByEvent(Long eventId);

    List<ParticipationRequest> findByEventAndIdIn(Long eventId, List<Long> requestIds);

    boolean existsByEventAndRequester(Long eventId, Long requesterId);

    int countByEventAndStatus(Long eventId, RequestStatus status);
}