package ru.yandex.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.event.EventServiceClient;
import ru.yandex.practicum.client.user.UserServiceClient;
import ru.yandex.practicum.dto.event.EventFullDto;
import ru.yandex.practicum.dto.event.EventState;
import ru.yandex.practicum.exception.ConflictException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.yandex.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.dto.request.RequestStatus;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.request.mapper.RequestMapper;
import ru.yandex.practicum.request.model.ParticipationRequest;
import ru.yandex.practicum.request.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RequestServiceImpl implements RequestService {

    ParticipationRequestRepository requestRepository;
    RequestMapper requestMapper;
    UserServiceClient userClient;
    EventServiceClient eventClient;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Создание запроса: user={}, event={}", userId, eventId);

        UserDto userDto = userClient.getUser(userId);

        EventFullDto eventDto = eventClient.getEvent(eventId);

        if (eventDto.getInitiator().getId().equals(userId)) {
            log.error("Инициатор не может подать заявку");
            throw new ConflictException("Инициатор события не может добавить запрос на участие");
        }

        if (eventDto.getState() != EventState.PUBLISHED) {
            log.error("Событие не опубликовано");
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.existsByEventAndRequester(eventId, userId)) {
            log.error("Запрос уже существует");
            throw new ConflictException("Нельзя добавить повторный запрос");
        }

        if (eventDto.getParticipantLimit() > 0) {
            int confirmed = requestRepository.countByEventAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmed >= eventDto.getParticipantLimit()) {
                log.error("Лимит участников исчерпан");
                throw new ConflictException("Достигнут лимит запросов на участие");
            }
        }

        RequestStatus status;
        if (eventDto.getParticipantLimit() == 0) {
            status = RequestStatus.CONFIRMED;
            log.info("Лимит участников = 0, статус CONFIRMED");
        } else {
            status = eventDto.getRequestModeration() ? RequestStatus.PENDING : RequestStatus.CONFIRMED;
            log.info("Лимит участников = {}, moderation = {}, статус {}",
                    eventDto.getParticipantLimit(),
                    eventDto.getRequestModeration(),
                    status);
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(eventId)
                .requester(userId)
                .status(status)
                .build();

        ParticipationRequest saved = requestRepository.save(request);
        log.info("Запрос создан, id: {}, статус: {}", saved.getId(), saved.getStatus());

        return requestMapper.toDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Запросы пользователя: {}", userId);

        try {
            userClient.getUser(userId);
        } catch (Exception e) {
            log.error("Пользователь не найден: {}", userId);
            throw new NotFoundException("Пользователь не найден");
        }

        return requestMapper.toDto(requestRepository.findByRequester(userId));
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

        if (!request.getRequester().equals(userId)) {
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

        EventFullDto eventDto = eventClient.getEvent(eventId);

        if (!eventDto.getInitiator().getId().equals(userId)) {
            log.error("Доступ запрещен");
            throw new NotFoundException("Событие не найдено");
        }

        return requestMapper.toDto(requestRepository.findByEvent(eventId));
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest request) {
        log.info("Обновление статусов: user={}, event={}, status={}", userId, eventId, request.getStatus());

        EventFullDto eventDto = eventClient.getEvent(eventId);

        if (!eventDto.getInitiator().getId().equals(userId)) {
            log.error("Доступ запрещен");
            return new EventRequestStatusUpdateResult(new ArrayList<>(), new ArrayList<>());
        }

        if (eventDto.getParticipantLimit() == 0 || !eventDto.getRequestModeration()) {
            log.info("Подтверждение не требуется");
            return new EventRequestStatusUpdateResult(new ArrayList<>(), new ArrayList<>());
        }

        List<ParticipationRequest> requests = requestRepository.findByEventAndIdIn(eventId, request.getRequestIds());

        for (ParticipationRequest pr : requests) {
            if (pr.getStatus() != RequestStatus.PENDING) {
                log.error("Запрос {} не в статусе PENDING", pr.getId());
                throw new ConflictException("Статус можно изменить только у заявок в состоянии ожидания");
            }
        }

        int confirmed = requestRepository.countByEventAndStatus(eventId, RequestStatus.CONFIRMED);
        int limit = eventDto.getParticipantLimit();

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

        log.info("Статусы обновлены: подтверждено={}, отклонено={}", confirmedList.size(), rejectedList.size());

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(requestMapper.toDto(confirmedList))
                .rejectedRequests(requestMapper.toDto(rejectedList))
                .build();
    }
}