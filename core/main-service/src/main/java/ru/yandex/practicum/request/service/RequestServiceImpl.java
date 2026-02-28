package ru.yandex.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.event.model.EventState;
import ru.yandex.practicum.event.repository.EventRepository;
import ru.yandex.practicum.exception.ConflictException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.yandex.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.yandex.practicum.request.dto.ParticipationRequestDto;
import ru.yandex.practicum.request.mapper.RequestMapper;
import ru.yandex.practicum.request.model.ParticipationRequest;
import ru.yandex.practicum.request.model.RequestStatus;
import ru.yandex.practicum.request.repository.ParticipationRequestRepository;
import ru.yandex.practicum.user.model.User;
import ru.yandex.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RequestServiceImpl implements RequestService {

    ParticipationRequestRepository requestRepository;
    UserRepository userRepository;
    EventRepository eventRepository;
    RequestMapper requestMapper;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Создание запроса: user={}, event={}", userId, eventId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден: {}", userId);
                    return new NotFoundException("Пользователь не найден");
                });

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Событие не найдено: {}", eventId);
                    return new NotFoundException("Событие не найдено");
                });

        if (event.getInitiator().getId().equals(userId)) {
            log.error("Инициатор не может подать заявку");
            throw new ConflictException("Инициатор события не может добавить запрос на участие");
        }

        if (event.getState() != EventState.PUBLISHED) {
            log.error("Событие не опубликовано");
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            log.error("Запрос уже существует");
            throw new ConflictException("Нельзя добавить повторный запрос");
        }

        if (event.getParticipantLimit() > 0) {
            int confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmed >= event.getParticipantLimit()) {
                log.error("Лимит участников исчерпан");
                throw new ConflictException("Достигнут лимит запросов на участие");
            }
        }

        RequestStatus status;
        if (event.getParticipantLimit() == 0) {
            status = RequestStatus.CONFIRMED;
            log.info("Лимит участников = 0, статус CONFIRMED");
        } else {
            status = event.getRequestModeration() ? RequestStatus.PENDING : RequestStatus.CONFIRMED;
            log.info("Лимит участников = {}, moderation = {}, статус {}",
                    event.getParticipantLimit(),
                    event.getRequestModeration(),
                    status);
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(user)
                .status(status)
                .build();

        ParticipationRequest saved = requestRepository.save(request);
        log.info("Запрос создан, id: {}, статус: {}", saved.getId(), saved.getStatus());

        return requestMapper.toDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Запросы пользователя: {}", userId);

        if (!userRepository.existsById(userId)) {
            log.error("Пользователь не найден: {}", userId);
            throw new NotFoundException("Пользователь не найден");
        }

        return requestMapper.toDto(requestRepository.findByRequesterId(userId));
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена запроса: user={}, request={}", userId, requestId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("Запрос не найден: {}", requestId);
                    return new NotFoundException("Запрос не найден");
                });

        if (!request.getRequester().getId().equals(userId)) {
            log.error("Доступ запрещен");
            throw new NotFoundException("Запрос не найден");
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest saved = requestRepository.save(request);
        log.info("Запрос отменен, id: {}", saved.getId());

        return requestMapper.toDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Запросы на событие: user={}, event={}", userId, eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Событие не найдено: {}", eventId);
                    return new NotFoundException("Событие не найдено");
                });

        if (!event.getInitiator().getId().equals(userId)) {
            log.error("Доступ запрещен");
            throw new NotFoundException("Событие не найдено");
        }

        return requestMapper.toDto(requestRepository.findByEventId(eventId));
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest request) {
        log.info("Обновление статусов: user={}, event={}, status={}", userId, eventId, request.getStatus());

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Событие не найдено: {}", eventId);
                    return new NotFoundException("Событие не найдено");
                });

        if (!event.getInitiator().getId().equals(userId)) {
            log.error("Доступ запрещен");
            return new EventRequestStatusUpdateResult(new ArrayList<>(), new ArrayList<>());
        }

        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            log.info("Подтверждение не требуется");
            return new EventRequestStatusUpdateResult(new ArrayList<>(), new ArrayList<>());
        }

        List<ParticipationRequest> requests = requestRepository.findByEventIdAndIdIn(eventId, request.getRequestIds());

        for (ParticipationRequest pr : requests) {
            if (pr.getStatus() != RequestStatus.PENDING) {
                log.error("Запрос {} не в статусе PENDING", pr.getId());
                throw new ConflictException("Статус можно изменить только у заявок в состоянии ожидания");
            }
        }

        int confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        int limit = event.getParticipantLimit();

        if (request.getStatus() == RequestStatus.CONFIRMED && confirmed >= limit) {
            log.error("Лимит участников уже достигнут: {}/{}", confirmed, limit);
            throw new ConflictException("Лимит участников уже достигнут");
        }

        List<ParticipationRequest> confirmedList = new ArrayList<>();
        List<ParticipationRequest> rejectedList = new ArrayList<>();

        if (request.getStatus() == RequestStatus.CONFIRMED) {
            for (ParticipationRequest pr : requests) {
                if (confirmed < limit) {
                    pr.setStatus(RequestStatus.CONFIRMED);
                    confirmedList.add(pr);
                    confirmed++;
                } else {
                    pr.setStatus(RequestStatus.REJECTED);
                    rejectedList.add(pr);
                }
            }
        } else {
            for (ParticipationRequest pr : requests) {
                pr.setStatus(RequestStatus.REJECTED);
                rejectedList.add(pr);
            }
        }

        requestRepository.saveAll(requests);

        long newConfirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        event.setConfirmedRequests(newConfirmedCount);
        eventRepository.save(event);

        log.info("Статусы обновлены: подтверждено={}, отклонено={}", confirmedList.size(), rejectedList.size());

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(requestMapper.toDto(confirmedList))
                .rejectedRequests(requestMapper.toDto(rejectedList))
                .build();
    }
}
