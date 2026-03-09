package ru.yandex.practicum.client.event;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.dto.event.EventFullDto;

import java.util.List;

@FeignClient(name = "event-service")
public interface EventServiceClient {

    @GetMapping("/events/by-ids")
    List<EventFullDto> getEventsByIds(@RequestParam("ids") List<Long> ids);

    @GetMapping("/events/{id}")
    EventFullDto getEvent(@PathVariable("id") Long id);

    @GetMapping("/events/categories/{catId}/exists")
    boolean existsEventsByCategoryId(@PathVariable("catId") Long catId);

    @GetMapping("/internal/events/{id}")
    EventFullDto getInternalEvent(@PathVariable("id") Long id);
}
