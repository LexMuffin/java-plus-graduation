package ru.yandex.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.event.EventFullDto;
import ru.yandex.practicum.dto.event.EventShortDto;
import ru.yandex.practicum.dto.event.PublicEventSearchParams;
import ru.yandex.practicum.event.service.EventService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PublicEventController {
    EventService eventService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/by-ids")
    public List<EventFullDto> getEventsByIds(@RequestParam("ids") List<Long> ids) {
        log.info("GET /events/by-ids with ids: {}", ids);
        return eventService.getEventsByIds(ids);
    }

    @GetMapping
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size,
            HttpServletRequest request) {

        log.info("GET /events: text={}, categories={}, paid={}", text, categories, paid);

        Pageable pageable = PageRequest.of(from / size, size);

        PublicEventSearchParams params = PublicEventSearchParams.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart != null ? LocalDateTime.parse(rangeStart, FORMATTER) : null)
                .rangeEnd(rangeEnd != null ? LocalDateTime.parse(rangeEnd, FORMATTER) : null)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .build();

        return eventService.findPublicEvents(params, pageable, request);
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent(@PathVariable Long id,
                                 HttpServletRequest request) {
        log.info("GET /events/{}", id);
        return eventService.getPublicEvent(id, request);
    }

    @GetMapping("/categories/{catId}/exists")
    public boolean existsEventsByCategoryId(@PathVariable Long catId) {
        log.info("GET /events/categories/{}/exists - проверка наличия событий для категории", catId);
        return eventService.existsEventsByCategoryId(catId);
    }
}