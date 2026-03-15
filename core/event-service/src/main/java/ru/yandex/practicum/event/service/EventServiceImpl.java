package ru.yandex.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.AnalyzerGrpcClient;
import ru.yandex.practicum.CollectorGrpcClient;
import ru.yandex.practicum.client.category.CategoryServiceClient;
import ru.yandex.practicum.client.request.RequestServiceClient;
import ru.yandex.practicum.client.user.UserServiceClient;
import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.event.mapper.EventMapper;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.event.repository.EventRepository;
import ru.yandex.practicum.ewm.grpc.stats.messages.ActionTypeProto;
import ru.yandex.practicum.ewm.grpc.stats.messages.RecommendedEventProto;
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
    UserServiceClient userClient;
    CategoryServiceClient categoryClient;
    RequestServiceClient requestClient;

    CollectorGrpcClient collectorClient;
    AnalyzerGrpcClient analyzerClient;

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

        EventFullDto fullDto = eventMapper.toFullDtoWithRating(savedEvent, 0L, 0.0);

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

        userClient.getUser(userId);

        Page<Event> events = eventRepository.findByInitiator(userId, pageable);
        List<Event> eventList = events.getContent();

        Map<Long, Double> ratingsMap = getRatingsForEvents(eventList);
        Map<Long, Long> confirmedMap = getConfirmedRequests(eventList);

        List<EventShortDto> dtos = eventList.stream()
                .map(event -> eventMapper.toShortDtoWithRating(
                        event,
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        ratingsMap.getOrDefault(event.getId(), 0.0)))
                .collect(Collectors.toList());

        enrichEventsWithCategories(dtos);

        return dtos;
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        log.info("Запрос события {} пользователя {}", eventId, userId);

        userClient.getUser(userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!event.getInitiator().equals(userId)) {
            log.warn("Событие {} не принадлежит юзеру {}", eventId, userId);
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        Long confirmedRequests = getConfirmedRequestsForEvent(eventId);
        Double rating = getRatingForEvent(eventId);

        EventFullDto dto = eventMapper.toFullDtoWithRating(event, confirmedRequests, rating);

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
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

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

        Long confirmedRequests = getConfirmedRequestsForEvent(eventId);
        Double rating = getRatingForEvent(eventId);

        EventFullDto dto = eventMapper.toFullDtoWithRating(updatedEvent, confirmedRequests, rating);

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

        Map<Long, Double> ratingsMap = getRatingsForEvents(eventList);
        Map<Long, Long> confirmedMap = getConfirmedRequests(eventList);

        List<EventFullDto> dtos = eventList.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toFullDtoWithRating(
                            event,
                            confirmedMap.getOrDefault(event.getId(), 0L),
                            ratingsMap.getOrDefault(event.getId(), 0.0)
                    );
                    return dto;
                })
                .collect(Collectors.toList());

        enrichFullEventsWithCategories(dtos);

        return dtos;
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request) {
        log.info("Админ обновление события: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

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

        Long confirmedRequests = getConfirmedRequestsForEvent(eventId);
        Double rating = getRatingForEvent(eventId);

        EventFullDto dto = eventMapper.toFullDtoWithRating(updatedEvent, confirmedRequests, rating);

        try {
            CategoryDto categoryDto = categoryClient.getCategory(updatedEvent.getCategory());
            dto.setCategory(categoryDto);
        } catch (Exception e) {
            log.warn("Не удалось загрузить данные категории для события {}", updatedEvent.getId());
        }

        return dto;
    }

    @Override
    public List<EventShortDto> findPublicEvents(PublicEventSearchParams params, Pageable pageable) {
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

            Map<Long, Double> ratingsMap = getRatingsForEvents(events);
            Map<Long, Long> confirmedMap = getConfirmedRequests(events);

            if (params.getOnlyAvailable() != null && params.getOnlyAvailable()) {
                events = events.stream()
                        .filter(e -> e.getParticipantLimit() == 0 ||
                                confirmedMap.getOrDefault(e.getId(), 0L) < e.getParticipantLimit())
                        .collect(Collectors.toList());
            }

            if (params.getSort() != null) {
                switch (params.getSort()) {
                    case "EVENT_DATE":
                        events.sort(Comparator.comparing(Event::getEventDate));
                        break;
                    case "RATING": // заменили VIEWS на RATING
                        events.sort((e1, e2) -> Double.compare(
                                ratingsMap.getOrDefault(e2.getId(), 0.0),
                                ratingsMap.getOrDefault(e1.getId(), 0.0)
                        ));
                        break;
                }
            }

            List<EventShortDto> dtos = events.stream()
                    .map(event -> eventMapper.toShortDtoWithRating(
                            event,
                            confirmedMap.getOrDefault(event.getId(), 0L),
                            ratingsMap.getOrDefault(event.getId(), 0.0)))
                    .collect(Collectors.toList());

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
    public EventFullDto getPublicEvent(Long eventId, Long userId, HttpServletRequest request) {
        log.info("Публичный запрос события: {}, пользователь: {}, IP: {}", eventId, userId, request.getRemoteAddr());

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        try {
            collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
            log.info("Отправлен просмотр события {} пользователем {}", eventId, userId);
        } catch (Exception e) {
            log.error("Ошибка при отправке просмотра в Collector: {}", e.getMessage());
        }

        Long confirmedRequests = getConfirmedRequestsForEvent(eventId);
        Double rating = getRatingForEvent(eventId);

        event.setConfirmedRequests(confirmedRequests);

        EventFullDto dto = eventMapper.toFullDtoWithRating(event, confirmedRequests, rating);

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

        Map<Long, Double> ratingsMap = getRatingsForEvents(events);
        Map<Long, Long> confirmedMap = getConfirmedRequests(events);

        Map<Long, Event> eventMap = events.stream()
                .collect(Collectors.toMap(Event::getId, event -> event));

        List<Event> sortedEvents = ids.stream()
                .map(eventMap::get)
                .filter(Objects::nonNull)
                .toList();

        List<EventFullDto> dtos = sortedEvents.stream()
                .map(event -> eventMapper.toFullDtoWithRating(
                        event,
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        ratingsMap.getOrDefault(event.getId(), 0.0)))
                .collect(Collectors.toList());

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

        Long confirmedRequests = getConfirmedRequestsForEvent(id);
        Double rating = getRatingForEvent(id);

        return eventMapper.toFullDtoWithRating(event, confirmedRequests, rating);
    }

    @Override
    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        log.info("Запрос рекомендаций для пользователя: {}, maxResults: {}", userId, maxResults);

        try {
            userClient.getUser(userId);
            return analyzerClient.getRecommendationsForUser(userId, maxResults).toList();
        } catch (Exception e) {
            log.error("Ошибка при получении рекомендаций: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional
    public void sendLike(long userId, long eventId) {
        log.info("Отправка лайка: пользователь {}, событие {}", userId, eventId);

        userClient.getUser(userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!requestClient.checkUserParticipated(userId, eventId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Пользователь может лайкать только посещённые мероприятия");
        }

        collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
        log.info("Лайк успешно отправлен");
    }

    private Double getRatingForEvent(Long eventId) {
        try {
            return analyzerClient.getInteractionsCount(List.of(eventId))
                    .map(RecommendedEventProto::getScore)
                    .findFirst()
                    .orElse(0.0);
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинг для события {}: {}", eventId, e.getMessage());
            return 0.0;
        }
    }

    private Map<Long, Double> getRatingsForEvents(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        try {
            return analyzerClient.getInteractionsCount(eventIds)
                    .collect(Collectors.toMap(
                            RecommendedEventProto::getEventId,
                            RecommendedEventProto::getScore
                    ));
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинги для событий: {}", e.getMessage());
            return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0.0));
        }
    }

    private Long getConfirmedRequestsForEvent(Long eventId) {
        try {
            return requestClient.getConfirmedRequests(eventId);
        } catch (Exception e) {
            log.warn("Не удалось получить подтвержденные запросы для события {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        try {
            return requestClient.getConfirmedRequestsBatch(eventIds);
        } catch (Exception e) {
            log.warn("Не удалось получить подтвержденные запросы: {}", e.getMessage());
            return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0L));
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