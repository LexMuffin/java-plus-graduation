package ru.yandex.practicum.request.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.request.service.RequestService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class InternalRequestController {

    private final RequestService requestService;

    @GetMapping("/events/{eventId}/confirmed-requests")
    public Long getConfirmedRequests(@PathVariable Long eventId) {
        log.info("GET внутренний запрос количества подтверждённых заявок для события {}", eventId);
        return requestService.getConfirmedRequests(eventId);
    }

    @PostMapping("/events/confirmed-requests/batch")
    public Map<Long, Long> getConfirmedRequestsBatch(@RequestBody List<Long> eventIds) {
        log.info("POST внутренний пакетный запрос для {} событий", eventIds.size());
        return requestService.getConfirmedRequestsBatch(eventIds);
    }
}
