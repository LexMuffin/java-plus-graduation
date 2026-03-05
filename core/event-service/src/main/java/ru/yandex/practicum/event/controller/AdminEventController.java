package ru.yandex.practicum.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.event.service.EventService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminEventController {

    EventService eventService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping
    public List<EventFullDto> getEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {

        log.info("GET /admin/events: users={}, states={}, categories={}", users, states, categories);

        List<EventState> stateList = null;
        if (states != null && !states.isEmpty()) {
            stateList = states.stream().map(EventState::valueOf).collect(Collectors.toList());
        }

        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart, FORMATTER) : null;
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd, FORMATTER) : null;

        AdminEventSearchParams params = AdminEventSearchParams.builder()
                .users(users)
                .states(stateList)
                .categories(categories)
                .rangeStart(start)
                .rangeEnd(end)
                .build();

        Pageable pageable = PageRequest.of(from / size, size);
        return eventService.findAdminEvents(params, pageable);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable Long eventId,
                                    @Valid @RequestBody UpdateEventAdminRequest request) {
        log.info("PATCH /admin/events/{}", eventId);
        return eventService.updateAdminEvent(eventId, request);
    }
}