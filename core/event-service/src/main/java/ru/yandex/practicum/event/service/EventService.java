package ru.yandex.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.ewm.grpc.stats.messages.RecommendedEventProto;

import java.util.List;

public interface EventService {

    EventFullDto createEvent(Long userId, NewEventDto dto);

    List<EventShortDto> getUserEvents(Long userId, Pageable pageable);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    List<EventFullDto> findAdminEvents(AdminEventSearchParams params, Pageable pageable);

    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> findPublicEvents(PublicEventSearchParams params, Pageable pageable);

    EventFullDto getPublicEvent(Long eventId, Long userId, HttpServletRequest request);

    List<EventFullDto> getEventsByIds(List<Long> ids);

    boolean existsEventsByCategoryId(Long catId);

    EventFullDto getEventById(Long id);

    List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults);

    void sendLike(long userId, long eventId);
}