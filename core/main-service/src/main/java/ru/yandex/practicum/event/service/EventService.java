package ru.yandex.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.event.dto.*;

import java.util.List;

public interface EventService {

    EventFullDto createEvent(Long userId, NewEventDto dto);

    List<EventShortDto> getUserEvents(Long userId, Pageable pageable);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    List<EventFullDto> findAdminEvents(List<Long> users, List<String> states,
                                       List<Long> categories, String rangeStart,
                                       String rangeEnd, Pageable pageable);

    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> findPublicEvents(String text, List<Long> categories,
                                         Boolean paid, String rangeStart,
                                         String rangeEnd, Boolean onlyAvailable,
                                         String sort, Pageable pageable,
                                         HttpServletRequest request);

    EventFullDto getPublicEvent(Long eventId, HttpServletRequest request);
}
