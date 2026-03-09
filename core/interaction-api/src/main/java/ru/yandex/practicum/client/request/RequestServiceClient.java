package ru.yandex.practicum.client.request;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestServiceClient {

    @GetMapping("/internal/events/{eventId}/confirmed-requests")
    Long getConfirmedRequests(@PathVariable("eventId") Long eventId);

    @PostMapping("/internal/events/confirmed-requests/batch")
    Map<Long, Long> getConfirmedRequestsBatch(@RequestBody List<Long> eventIds);
}
