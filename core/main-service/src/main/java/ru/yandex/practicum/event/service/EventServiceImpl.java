package ru.yandex.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.category.model.Category;
import ru.yandex.practicum.category.repository.CategoryRepository;
import ru.yandex.practicum.event.dto.*;
import ru.yandex.practicum.event.mapper.EventMapper;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.event.model.EventState;
import ru.yandex.practicum.event.model.EventStateAction;
import ru.yandex.practicum.event.repository.EventRepository;
import ru.yandex.practicum.exception.ConflictException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.user.model.User;
import ru.yandex.practicum.user.repository.UserRepository;

import javax.xml.stream.Location;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    EventRepository eventRepository;
    UserRepository userRepository;
    CategoryRepository categoryRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        log.info("Создание события пользователем: {}", userId);

        // проверка даты
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            log.error("Слишком ранняя дата: {}", dto.getEventDate());
            throw new ConflictException("Дата должна быть не раньше чем через 2 часа");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден: {}", userId);
                    return new NotFoundException("Пользователь не найден");
                });

        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> {
                    log.error("Категория не найдена: {}", dto.getCategory());
                    return new NotFoundException("Категория не найдена");
                });

        Event event = EventMapper.INSTANCE.toEntity(dto);
        event.setInitiator(user);
        event.setCategory(category);

        if (event.getPaid() == null) event.setPaid(false);
        if (event.getParticipantLimit() == null) event.setParticipantLimit(0);
        if (event.getRequestModeration() == null) event.setRequestModeration(true);

        Event saved = eventRepository.save(event);
        log.info("Событие создано, id: {}", saved.getId());

        return EventMapper.INSTANCE.toFullDto(saved);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        log.info("Запрос событий пользователя: {}", userId);

        if (!userRepository.existsById(userId)) {
            log.error("Пользователь не найден: {}", userId);
            throw new NotFoundException("Пользователь не найден");
        }

        return EventMapper.INSTANCE.toShortDto(eventRepository.findByInitiatorId(userId, pageable).getContent());
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        log.info("Запрос события {} пользователя {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Событие не найдено: {}", eventId);
                    return new NotFoundException("Событие не найдено");
                });

        if (!event.getInitiator().getId().equals(userId)) {
            log.error("Доступ запрещен: событие {} не принадлежит пользователю {}", eventId, userId);
            throw new NotFoundException("Событие не найдено");
        }

        return EventMapper.INSTANCE.toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("Обновление события {} пользователем {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Событие не найдено: {}", eventId);
                    return new NotFoundException("Событие не найдено");
                });

        if (!event.getInitiator().getId().equals(userId)) {
            log.error("Доступ запрещен: событие {} не принадлежит пользователю {}", eventId, userId);
            throw new NotFoundException("Событие не найдено");
        }

        // проверка статуса
        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            log.error("Нельзя изменить событие в статусе: {}", event.getState());
            throw new ConflictException("Можно изменять только ожидающие или отмененные события");
        }

        updateEventFields(event, request);

        // проверка даты
        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            log.error("Слишком ранняя дата: {}", event.getEventDate());
            throw new ConflictException("Дата должна быть не раньше чем через 2 часа");
        }

        // обработка статуса
        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updated = eventRepository.save(event);
        log.info("Событие обновлено, id: {}", updated.getId());

        return EventMapper.INSTANCE.toFullDto(updated);
    }

    @Override
    public List<EventFullDto> findAdminEvents(List<Long> users, List<String> states,
                                              List<Long> categories, String rangeStart,
                                              String rangeEnd, Pageable pageable) {
        log.info("Админ поиск событий");

        List<EventState> stateList = null;
        if (states != null) {
            stateList = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }

        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, FORMATTER) : null;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, FORMATTER) : null;

        return EventMapper.INSTANCE.toFullDto(eventRepository.findAdminEvents(users, stateList, categories, start, end, pageable).getContent());
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request) {
        log.info("Админ обновление события: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Событие не найдено: {}", eventId);
                    return new NotFoundException("Событие не найдено");
                });

        updateEventFields(event, request);

        // проверка даты для публикации
        if (request.getStateAction() == EventStateAction.PUBLISH_EVENT) {
            if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                log.error("Слишком поздно для публикации");
                throw new ConflictException("Дата начала должна быть не раньше чем через час");
            }
            if (event.getState() != EventState.PENDING) {
                log.error("Нельзя опубликовать событие в статусе: {}", event.getState());
                throw new ConflictException("Можно публиковать только ожидающие события");
            }
        }

        // обработка статуса
        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case PUBLISH_EVENT:
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        log.error("Нельзя отклонить опубликованное событие");
                        throw new ConflictException("Нельзя отклонить опубликованное событие");
                    }
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updated = eventRepository.save(event);
        log.info("Событие обновлено админом, id: {}", updated.getId());

        return EventMapper.INSTANCE.toFullDto(updated);
    }

    @Override
    public List<EventShortDto> findPublicEvents(String text, List<Long> categories,
                                                Boolean paid, String rangeStart,
                                                String rangeEnd, Boolean onlyAvailable,
                                                String sort, Pageable pageable) {
        log.info("Публичный поиск событий");

        LocalDateTime start = rangeStart != null ?
                LocalDateTime.parse(rangeStart, FORMATTER) : LocalDateTime.now();

        LocalDateTime end = rangeEnd != null ?
                LocalDateTime.parse(rangeEnd, FORMATTER) : LocalDateTime.now().plusYears(100);

        List<Event> events = eventRepository.findPublicEvents(text, categories, paid, start, end, pageable).getContent();

        // фильтр только доступных
        if (onlyAvailable != null && onlyAvailable) {
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0 ||
                            e.getConfirmedRequests() < e.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        // сортировка
        if (sort != null) {
            switch (sort) {
                case "EVENT_DATE":
                    events.sort((e1, e2) -> e1.getEventDate().compareTo(e2.getEventDate()));
                    break;
                case "VIEWS":
                    events.sort((e1, e2) -> e1.getViews().compareTo(e2.getViews()));
                    break;
            }
        }

        return EventMapper.INSTANCE.toShortDto(events);
    }

    @Override
    public EventFullDto getPublicEvent(Long eventId) {
        log.info("Публичный запрос события: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Событие не найдено: {}", eventId);
                    return new NotFoundException("Событие не найдено");
                });

        if (event.getState() != EventState.PUBLISHED) {
            log.error("Событие не опубликовано: {}", eventId);
            throw new NotFoundException("Событие не найдено");
        }

        // увеличиваем просмотры
        event.setViews(event.getViews() == null ? 1 : event.getViews() + 1);
        eventRepository.save(event);

        return EventMapper.INSTANCE.toFullDto(event);
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getLocation() != null) event.setLocation(EventMapper.INSTANCE.toLocation(request.getLocation()));
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }

    private void updateEventFields(Event event, UpdateEventUserRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getLocation() != null) event.setLocation(EventMapper.INSTANCE.toLocation(request.getLocation()));
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }
}
