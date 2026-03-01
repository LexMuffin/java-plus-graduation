package ru.yandex.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.category.model.Category;
import ru.yandex.practicum.client.category.CategoryServiceClient;
import ru.yandex.practicum.client.stats.StatsServiceClient;
import ru.yandex.practicum.client.user.UserServiceClient;
import ru.yandex.practicum.dto.EndpointHitDto;
import ru.yandex.practicum.dto.ViewStatsDto;
import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.event.mapper.EventMapper;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.event.repository.EventRepository;
import ru.yandex.practicum.exception.ConflictException;
import ru.yandex.practicum.exception.NotFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventServiceImpl implements EventService {

    EventRepository eventRepository;
    EventMapper eventMapper;
    StatsServiceClient statsClient;
    UserServiceClient userClient;
    CategoryServiceClient categoryClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        log.info("Создание события пользователем: {}", userId);

        UserDto userDto = userClient.getUser(userId);

        CategoryDto categoryDto = categoryClient.getCategory(dto.getCategory());

        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            log.warn("Слишком ранняя дата: {}", dto.getEventDate());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Поле eventDate: дата должна быть не раньше чем через 2 часа. Текущее значение: " + dto.getEventDate());
        }

        Event event = eventMapper.toEntity(dto);
        event.setInitiator(userDto.getId());
        event.setCategory(Category.builder().id(categoryDto.getId()).build());

        if (event.getPaid() == null) event.setPaid(false);
        if (event.getParticipantLimit() == null) event.setParticipantLimit(0);
        if (event.getRequestModeration() == null) event.setRequestModeration(true);

        Event savedEvent = eventRepository.save(event);
        log.info("Событие создано, id: {}", savedEvent.getId());
        return eventMapper.toFullDto(savedEvent);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        log.info("Запрос событий пользователя: {}", userId);

        try {
            userClient.getUser(userId);
        } catch (Exception e) {
            log.warn("Пользователь не найден: {}", userId);
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        Page<Event> events = eventRepository.findByInitiator(userId, pageable);
        return eventMapper.toShortDto(events.getContent());
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        log.info("Запрос события {} пользователя {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Событие не найдено: {}", eventId);
                    return new NotFoundException("Событие с id=" + eventId + " не найдено");
                });

        if (!event.getInitiator().equals(userId)) {
            log.warn("Событие {} не принадлежит юзеру {}", eventId, userId);
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        return eventMapper.toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("Обновление события {} пользователем {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Событие не найдено: {}", eventId);
                    return new NotFoundException("Событие с id=" + eventId + " не найдено");
                });

        if (!event.getInitiator().equals(userId)) {
            log.warn("Событие {} не принадлежит пользователю {}", eventId, userId);
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            log.warn("Неверный статус: {}", event.getState());
            throw new ConflictException("Можно изменять только ожидающие или отмененные события");
        }

        updateEventFields(event, request);

        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            log.warn("Слишком ранняя дата: {}", event.getEventDate());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Поле eventDate: дата должна быть не раньше чем через 2 часа. Текущее значение: " + event.getEventDate());
        }

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие обновлено пользователем, id: {}", updatedEvent.getId());
        return eventMapper.toFullDto(updatedEvent);
    }

    @Override
    public List<EventFullDto> findAdminEvents(List<Long> users, List<String> states,
                                              List<Long> categories, String rangeStart,
                                              String rangeEnd, Pageable pageable) {
        log.info("Админ поиск");

        List<EventState> stateList = null;
        if (states != null && !states.isEmpty()) {
            stateList = states.stream().map(EventState::valueOf).collect(Collectors.toList());
        }

        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, FORMATTER) : null;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, FORMATTER) : null;

        Page<Event> events = eventRepository.findAdminEvents(users, stateList, categories, start, end, pageable);
        return eventMapper.toFullDto(events.getContent());
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request) {
        log.info("Админ обновление события: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Событие не найдено: {}", eventId);
                    return new NotFoundException("Событие с id=" + eventId + " не найдено");
                });

        updateEventFields(event, request);

        if (request.getStateAction() == EventStateAction.PUBLISH_EVENT) {
            if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                log.warn("Слишком поздно для публикации");
                throw new ConflictException("Нельзя опубликовать: до события осталось меньше часа");
            }
            if (event.getState() != EventState.PENDING) {
                log.warn("Нельзя опубликовать событие в статусе: {}", event.getState());
                throw new ConflictException("Нельзя опубликовать событие в статусе: " + event.getState());
            }
        }

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case PUBLISH_EVENT -> {
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                }
                case REJECT_EVENT -> {
                    if (event.getState() == EventState.PUBLISHED) {
                        log.warn("Нельзя отклонить опубликованное событие");
                        throw new ConflictException("Нельзя отклонить опубликованное событие");
                    }
                    event.setState(EventState.CANCELED);
                }
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие обновлено администратором, id: {}", updatedEvent.getId());

        EventFullDto dto = eventMapper.toFullDto(updatedEvent);
        dto.setConfirmedRequests(event.getConfirmedRequests());
        return dto;
    }

    @Override
    public List<EventShortDto> findPublicEvents(String text, List<Long> categories,
                                                Boolean paid, String rangeStart,
                                                String rangeEnd, Boolean onlyAvailable,
                                                String sort, Pageable pageable,
                                                HttpServletRequest request) {
        log.info("Публичный поиск: text={}, categories={}, paid={}", text, categories, paid);

        try {
            LocalDateTime start;
            LocalDateTime end;

            if (rangeStart == null || rangeStart.isBlank()) {
                start = LocalDateTime.now();
            } else {
                try {
                    start = LocalDateTime.parse(rangeStart, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (Exception e) {
                    log.warn("Ошибка парсинга rangeStart: {}", rangeStart);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Неверный формат даты. Используйте: yyyy-MM-dd HH:mm:ss");
                }
            }

            if (rangeEnd == null || rangeEnd.isBlank()) {
                end = LocalDateTime.now().plusYears(100);
            } else {
                try {
                    end = LocalDateTime.parse(rangeEnd, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (Exception e) {
                    log.warn("Ошибка парсинга rangeEnd: {}", rangeEnd);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Неверный формат даты. Используйте: yyyy-MM-dd HH:mm:ss");
                }
            }

            if (start.isAfter(end)) {
                log.warn("start после end: {} > {}", start, end);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Дата начала должна быть раньше даты окончания");
            }

            Page<Event> eventsPage = eventRepository.findPublicEvents(text, categories, paid, start, end, pageable);
            List<Event> events = new ArrayList<>(eventsPage.getContent());

            try {
                String queryString = request.getQueryString();
                String uri = request.getRequestURI() + (queryString != null ? "?" + queryString : "");
                String ip = request.getRemoteAddr();

                EndpointHitDto hitDto = EndpointHitDto.builder()
                        .app("main-service")
                        .uri(uri)
                        .ip(ip)
                        .timestamp(LocalDateTime.now())
                        .build();

                statsClient.saveHit(hitDto);
                log.debug("Хит отправлен в статистику для поиска событий");
            } catch (Exception e) {
                log.error("Ошибка при отправке хита на поиск в статистику: {}", e.getMessage());
            }

            List<Long> eventIds = events.stream()
                    .map(Event::getId)
                    .collect(Collectors.toList());

            Map<Long, Long> viewsMap = getViewsForEvents(eventIds);

            events.forEach(event -> {
                Long views = viewsMap.get(event.getId());
                if (views != null) {
                    event.setViews(views);
                }
            });

            if (onlyAvailable != null && onlyAvailable) {
                events = events.stream()
                        .filter(e -> e.getParticipantLimit() == 0 ||
                                e.getConfirmedRequests() < e.getParticipantLimit())
                        .collect(Collectors.toList());
            }

            if (sort != null && !events.isEmpty()) {
                switch (sort) {
                    case "EVENT_DATE" -> events.sort(Comparator.comparing(Event::getEventDate));
                    case "VIEWS" -> events.sort(Comparator.comparing(Event::getViews).reversed());
                    default -> log.warn("Неизвестная сортировка: {}", sort);
                }
            }

            return eventMapper.toShortDto(events);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка в публичном поиске", e);
            throw new RuntimeException("Ошибка при поиске событий", e);
        }
    }

    @Override
    @Transactional
    public EventFullDto getPublicEvent(Long eventId, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        log.info("Публичный запрос события: {}, IP: {}", eventId, ip);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Событие не найдено: {}", eventId);
                    return new NotFoundException("Событие с id=" + eventId + " не найдено");
                });

        if (event.getState() != EventState.PUBLISHED) {
            log.warn("Событие не опубликовано: {}", eventId);
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        try {
            EndpointHitDto hitDto = EndpointHitDto.builder()
                    .app("main-service")
                    .uri("/events/" + eventId)
                    .ip(ip)
                    .timestamp(LocalDateTime.now())
                    .build();

            statsClient.saveHit(hitDto);
            log.debug("Хит отправлен в статистику для события {}", eventId);
        } catch (Exception e) {
            log.error("Ошибка при отправке хита в статистику: {}", e.getMessage());
        }

        try {
            LocalDateTime start = event.getPublishedOn() != null ? event.getPublishedOn() :
                    (event.getCreatedOn() != null ? event.getCreatedOn() :
                            LocalDateTime.now().minusYears(100));
            LocalDateTime end = LocalDateTime.now().plusYears(100);

            List<String> uris = List.of("/events/" + eventId);

            List<ViewStatsDto> stats = statsClient.getStats(
                    start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    uris,
                    true
            );

            long views = stats.isEmpty() ? 0 : stats.get(0).getHits();
            event.setViews(views);

        } catch (Exception e) {
            log.error("Ошибка при получении статистики просмотров: {}", e.getMessage());
            event.setViews(event.getViews() == null ? 1 : event.getViews() + 1);
        }

        eventRepository.save(event);

        return eventMapper.toFullDto(event);
    }

    private void updateEventFields(Event event, UpdateEventUserRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getCategory() != null) {
            CategoryDto categoryDto = categoryClient.getCategory(request.getCategory());
            event.setCategory(Category.builder().id(categoryDto.getId()).build());
        }
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getLocation() != null) event.setLocation(eventMapper.toLocation(request.getLocation()));
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getCategory() != null) {
            CategoryDto categoryDto = categoryClient.getCategory(request.getCategory());
            event.setCategory(Category.builder().id(categoryDto.getId()).build());
        }
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getLocation() != null) event.setLocation(eventMapper.toLocation(request.getLocation()));
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }

    private Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> viewsMap = new HashMap<>();

        for (Long eventId : eventIds) {
            viewsMap.put(eventId, 0L);
        }

        try {
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            LocalDateTime start = LocalDateTime.now().minusYears(100);
            LocalDateTime end = LocalDateTime.now().plusYears(100);

            String startStr = start.format(FORMATTER);
            String endStr = end.format(FORMATTER);

            List<ViewStatsDto> stats = statsClient.getStats(startStr, endStr, uris, false);

            for (ViewStatsDto stat : stats) {
                try {
                    Long eventId = extractEventIdFromUri(stat.getUri());
                    if (eventId != null) {
                        viewsMap.put(eventId, stat.getHits());
                    }
                } catch (Exception e) {
                    log.warn("Не удалось распарсить ID события из URI: {}", stat.getUri());
                }
            }
        } catch (Exception ex) {
            log.warn("Сервис статистики недоступен, используем значения по умолчанию (0)");
        }

        return viewsMap;
    }

    private Long extractEventIdFromUri(String uri) {
        if (uri == null || !uri.startsWith("/events/")) {
            return null;
        }
        try {
            return Long.parseLong(uri.substring("/events/".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}