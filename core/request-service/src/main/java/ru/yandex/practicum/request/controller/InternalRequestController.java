package ru.yandex.practicum.request.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.request.service.RequestService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/events")
public class InternalRequestController {

    private final RequestService requestService;

    @GetMapping("/{eventId}/confirmed-requests")
    public Long getConfirmedRequests(@PathVariable Long eventId) {
        log.info("GET внутренний запрос количества подтверждённых заявок для события {}", eventId);
        return requestService.getConfirmedRequests(eventId);
    }
}
