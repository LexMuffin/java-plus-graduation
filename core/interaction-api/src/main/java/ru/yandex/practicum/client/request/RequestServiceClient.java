package ru.yandex.practicum.client.request;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "request-service")
public interface RequestServiceClient {

    @GetMapping("/internal/events/{eventId}/confirmed-requests")
    Long getConfirmedRequests(@PathVariable("eventId") Long eventId);
}
