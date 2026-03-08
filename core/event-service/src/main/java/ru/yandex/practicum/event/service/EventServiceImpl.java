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
import ru.yandex.practicum.client.category.CategoryServiceClient;
import ru.yandex.practicum.client.request.RequestServiceClient;
import ru.yandex.practicum.client.stats.StatsServiceClient;
import ru.yandex.practicum.client.user.UserServiceClient;
import ru.yandex.practicum.dto.stats.EndpointHitDto;
import ru.yandex.practicum.dto.stats.ViewStatsDto;
import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.event.mapper.EventMapper;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.event.repository.EventRepository;
import ru.yandex.practicum.exception.ConflictException;
import ru.yandex.practicum.exception.NotFoundException;

import java.time.LocalDateTime;
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
    RequestServiceClient requestClient;

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
        event.setCategory(categoryDto.getId());
        event.setConfirmedRequests(0L);

        if (event.getPaid() == null) event.setPaid(false);
        if (event.getParticipantLimit() == null) event.setParticipantLimit(0);
        if (event.getRequestModeration() == null) event.setRequestModeration(true);

        Event savedEvent = eventRepository.save(event);
        log.info("Событие создано, id: {}", savedEvent.getId());

        EventFullDto fullDto = eventMapper.toFullDto(savedEvent);
        try {
            CategoryDto fullCategoryDto = categoryClient.getCategory(savedEvent.getCategory());
            fullDto.setCategory(fullCategoryDto);
        } catch (Exception e) {
            log.warn("Не удалось загрузить данные категории для события {}", savedEvent.getId());
        }

        return fullDto;
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
        List<EventShortDto> dtos = eventMapper.toShortDto(events.getContent());

        enrichEventsWithCategories(dtos);

        return dtos;
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

        try {
            Long confirmedRequests = requestClient.getConfirmedRequests(eventId);
            event.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0L);
        } catch (Exception e) {
            log.warn("Не удалось получить количество подтверждённых запросов для события {}", eventId);
        }

        EventFullDto dto = eventMapper.toFullDto(event);

        try {
            CategoryDto categoryDto = categoryClient.getCategory(event.getCategory());
            dto.setCategory(categoryDto);
        } catch (Exception e) {
            log.warn("Не удалось загрузить данные категории для события {}", eventId);
        }

        return dto;
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

        EventFullDto dto = eventMapper.toFullDto(updatedEvent);

        try {
            CategoryDto categoryDto = categoryClient.getCategory(updatedEvent.getCategory());
            dto.setCategory(categoryDto);
        } catch (Exception e) {
            log.warn("Не удалось загрузить данные категории для события {}", updatedEvent.getId());
        }

        return dto;
    }

    @Override
    public List<EventFullDto> findAdminEvents(AdminEventSearchParams params, Pageable pageable) {
        log.info("Админ поиск с параметрами: {}", params);

        Page<Event> events = eventRepository.findAdminEvents(
                params.getUsers(),
                params.getStates(),
                params.getCategories(),
                params.getRangeStart(),
                params.getRangeEnd(),
                pageable
        );
        List<Event> eventList = events.getContent();

        addViewsToEvents(eventList);

        eventList.forEach(event -> {
            try {
                Long confirmedRequests = requestClient.getConfirmedRequests(event.getId());
                event.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0L);
            } catch (Exception e) {
                log.warn("Не удалось получить количество подтверждённых запросов для события {}", event.getId());
            }
        });

        List<EventFullDto> dtos = eventMapper.toFullDto(eventList);
        enrichFullEventsWithCategories(dtos);

        return dtos;
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
        dto.setConfirmedRequests(updatedEvent.getConfirmedRequests());

        CategoryDto categoryDto = null;
        try {
            categoryDto = categoryClient.getCategory(updatedEvent.getCategory());
        } catch (Exception e) {
            log.warn("Не удалось загрузить данные категории для события {}", updatedEvent.getId());
        }

        return dto;
    }

    @Override
    public List<EventShortDto> findPublicEvents(PublicEventSearchParams params, Pageable pageable, HttpServletRequest request) {
        log.info("Публичный поиск: {}", params);

        try {
            if (params.getRangeStart() != null && params.getRangeEnd() != null
                    && params.getRangeStart().isAfter(params.getRangeEnd())) {
                log.warn("start после end: {} > {}", params.getRangeStart(), params.getRangeEnd());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Дата начала должна быть раньше даты окончания");
            }

            LocalDateTime rangeStart = params.getRangeStart() != null ?
                    params.getRangeStart() : LocalDateTime.now();
            LocalDateTime rangeEnd = params.getRangeEnd() != null ?
                    params.getRangeEnd() : LocalDateTime.now().plusYears(100);

            Page<Event> eventsPage = eventRepository.findPublicEvents(
                    params.getText(),
                    params.getCategories(),
                    params.getPaid(),
                    rangeStart,
                    rangeEnd,
                    pageable
            );
            List<Event> events = new ArrayList<>(eventsPage.getContent());

            sendHitToStats("/events", request.getRemoteAddr());

            events = enrichEventsWithStats(events);

            events = filterAvailableEvents(events, params.getOnlyAvailable());

            events = sortEvents(events, params.getSort());

            List<EventShortDto> dtos = eventMapper.toShortDto(events);
            enrichEventsWithCategories(dtos);

            return dtos;

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
        log.info("Публичный запрос события: {}, IP: {}", eventId, request.getRemoteAddr());

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        sendHitToStats("/events/" + eventId, request.getRemoteAddr());

        long views = getViewsForEvent(eventId);
        event.setViews(views);

        try {
            Long confirmedRequests = requestClient.getConfirmedRequests(eventId);
            event.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0L);
        } catch (Exception e) {
            log.warn("Не удалось получить подтвержденные запросы: {}", e.getMessage());
            event.setConfirmedRequests(0L);
        }

        EventFullDto dto = eventMapper.toFullDto(event);

        try {
            CategoryDto categoryDto = categoryClient.getCategory(event.getCategory());
            dto.setCategory(categoryDto);
        } catch (Exception e) {
            log.warn("Не удалось загрузить категорию: {}", e.getMessage());
        }

        return dto;
    }

    @Override
    public List<EventFullDto> getEventsByIds(List<Long> ids) {
        log.info("Получение событий по списку ID: {}", ids);

        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Event> events = eventRepository.findAllById(ids);

        Map<Long, Long> viewsMap = getViewsForEvents(ids);

        Map<Long, Long> confirmedRequestsMap = requestClient.getConfirmedRequestsBatch(ids);

        events.forEach(event -> {
            Long views = viewsMap.get(event.getId());
            if (views != null) {
                event.setViews(views);
            }

            Long confirmed = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
            event.setConfirmedRequests(confirmed);
        });

        Map<Long, Event> eventMap = events.stream()
                .collect(Collectors.toMap(Event::getId, event -> event));

        List<Event> sortedEvents = ids.stream()
                .map(eventMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<EventFullDto> dtos = eventMapper.toFullDto(sortedEvents);
        enrichFullEventsWithCategories(dtos);

        log.info("Найдено событий: {} из {}", dtos.size(), ids.size());
        return dtos;
    }

    @Override
    public boolean existsEventsByCategoryId(Long catId) {
        log.info("Проверка существования событий для категории: {}", catId);
        return eventRepository.existsByCategory(catId);
    }

    @Override
    public EventFullDto getEventById(Long id) {
        log.info("Получение события по ID для внутренних вызовов: {}", id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено"));

        return eventMapper.toFullDto(event);
    }

    private void sendHitToStats(String uri, String ip) {
        try {
            EndpointHitDto hitDto = EndpointHitDto.builder()
                    .app("ewm-main-service")
                    .uri(uri)
                    .ip(ip)
                    .timestamp(LocalDateTime.now())
                    .build();
            statsClient.saveHit(hitDto);
            log.debug("Хит отправлен: {}", uri);
        } catch (Exception e) {
            log.error("Ошибка при отправке хита: {}", e.getMessage());
        }
    }

    private void setViewsForEvent(Event event) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(100);
            LocalDateTime end = LocalDateTime.now().plusYears(1);

            List<ViewStatsDto> stats = statsClient.getStats(
                    start,
                    end,
                    List.of("/events/" + event.getId()),
                    true
            );

            long views = stats.isEmpty() ? 0 : stats.get(0).getHits();
            event.setViews(views);
            log.info("Установлено views = {} для события {}", views, event.getId());
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage());
            event.setViews(0L);
        }
    }

    private List<Event> enrichEventsWithStats(List<Event> events) {
        if (events.isEmpty()) return events;

        addViewsToEvents(events);

        try {
            Map<Long, Long> confirmedMap = requestClient.getConfirmedRequestsBatch(
                    events.stream().map(Event::getId).collect(Collectors.toList())
            );
            events.forEach(e -> e.setConfirmedRequests(confirmedMap.getOrDefault(e.getId(), 0L)));
        } catch (Exception ex) {
            log.warn("Не удалось получить подтвержденные запросы: {}", ex.getMessage());
            events.forEach(e -> e.setConfirmedRequests(0L));
        }

        return events;
    }

    private List<Event> filterAvailableEvents(List<Event> events, Boolean onlyAvailable) {
        if (onlyAvailable == null || !onlyAvailable) {
            return events;
        }

        return events.stream()
                .filter(e -> e.getParticipantLimit() == 0 ||
                        e.getConfirmedRequests() < e.getParticipantLimit())
                .collect(Collectors.toList());
    }

    private List<Event> sortEvents(List<Event> events, String sort) {
        if (sort == null || events.isEmpty()) {
            return events;
        }

        switch (sort) {
            case "EVENT_DATE":
                events.sort(Comparator.comparing(Event::getEventDate));
                break;
            case "VIEWS":
                events.sort(Comparator.comparing(Event::getViews).reversed());
                break;
            default:
                log.warn("Неизвестная сортировка: {}", sort);
        }

        return events;
    }

    private void addViewsToEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        LocalDateTime start = LocalDateTime.now().minusYears(100);
        LocalDateTime end = LocalDateTime.now().plusYears(1);

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    start,
                    end,
                    uris,
                    true
            );

            Map<Long, Long> viewsMap = stats.stream()
                    .collect(Collectors.toMap(
                            stat -> extractEventIdFromUri(stat.getUri()),
                            ViewStatsDto::getHits,
                            (existing, replacement) -> existing
                    ));

            events.forEach(event -> {
                Long views = viewsMap.getOrDefault(event.getId(), 0L);
                event.setViews(views);
            });

        } catch (Exception e) {
            log.warn("Не удалось получить статистику просмотров: {}", e.getMessage());
            events.forEach(event -> event.setViews(0L));
        }
    }

    private void updateEventFields(Event event, UpdateEventUserRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getCategory() != null) {
            CategoryDto categoryDto = categoryClient.getCategory(request.getCategory());
            event.setCategory(categoryDto.getId());
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
            event.setCategory(categoryDto.getId());
        }
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getLocation() != null) event.setLocation(eventMapper.toLocation(request.getLocation()));
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }

    private Long getViewsForEvent(Long eventId) {
        try {
            LocalDateTime start = LocalDateTime.now().minusMinutes(5);
            LocalDateTime end = LocalDateTime.now().plusMinutes(5);

            List<ViewStatsDto> stats = statsClient.getStats(
                    start,
                    end,
                    List.of("/events/" + eventId),
                    true
            );

            Long views = stats.isEmpty() ? 0 : stats.get(0).getHits();
            log.info("Для события {} получено views = {} из статистики", eventId, views);
            return views;
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage());
            return 0L;
        }
    }

    private Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            LocalDateTime start = LocalDateTime.now().minusYears(100);
            LocalDateTime end = LocalDateTime.now().plusYears(1);

            List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, true);

            return stats.stream()
                    .collect(Collectors.toMap(
                            stat -> extractEventIdFromUri(stat.getUri()),
                            ViewStatsDto::getHits,
                            (existing, replacement) -> existing
                    ));

        } catch (Exception ex) {
            log.warn("Сервис статистики недоступен, используем значения по умолчанию (0)");
            return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0L));
        }
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

    private void enrichEventsWithCategories(List<EventShortDto> dtos) {
        Set<Long> categoryIds = dtos.stream()
                .map(dto -> dto.getCategory().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (categoryIds.isEmpty()) return;

        Map<Long, CategoryDto> categoryMap = Optional.of(categoryIds)
                .map(ArrayList::new)
                .map(categoryClient::getCategoriesBatch)
                .orElse(Collections.emptyMap());

        dtos.stream()
                .filter(dto -> dto.getCategory() != null)
                .forEach(dto -> Optional.ofNullable(categoryMap.get(dto.getCategory().getId()))
                        .ifPresent(dto::setCategory));
    }

    private void enrichFullEventsWithCategories(List<EventFullDto> dtos) {
        Set<Long> categoryIds = dtos.stream()
                .map(dto -> dto.getCategory() != null ? dto.getCategory().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (categoryIds.isEmpty()) return;

        try {
            Map<Long, CategoryDto> categoryMap = categoryClient.getCategoriesBatch(new ArrayList<>(categoryIds));

            dtos.stream()
                    .filter(dto -> dto.getCategory() != null)
                    .forEach(dto -> Optional.ofNullable(categoryMap.get(dto.getCategory().getId()))
                            .ifPresent(dto::setCategory));
        } catch (Exception e) {
            log.warn("Не удалось получить категории пакетно: {}", e.getMessage());
        }
    }
}