package ru.yandex.practicum.client.event;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.dto.event.EventFullDto;

import java.util.List;

@FeignClient(name = "event-service", path = "/events")
public interface EventServiceClient {

    @GetMapping("/{id}")
    EventFullDto getEvent(@PathVariable("id") Long id);

    @GetMapping("/by-ids")
    List<EventFullDto> getEventsByIds(@RequestParam("ids") List<Long> ids);
}
