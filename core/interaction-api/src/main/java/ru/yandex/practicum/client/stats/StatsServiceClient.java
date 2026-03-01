package ru.yandex.practicum.client.stats;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.client.fallback.StatsServiceClientFallback;
import ru.yandex.practicum.dto.EndpointHitDto;
import ru.yandex.practicum.dto.ViewStatsDto;

import java.util.List;

@FeignClient(name = "stats-service", path = "/stats", fallback = StatsServiceClientFallback.class)
public interface StatsServiceClient {

    @PostMapping("/hit")
    void saveHit(@RequestBody EndpointHitDto hitDto);

    @GetMapping
    List<ViewStatsDto> getStats(
            @RequestParam("start") String start,
            @RequestParam("end") String end,
            @RequestParam("uris") List<String> uris,
            @RequestParam("unique") Boolean unique
    );
}
