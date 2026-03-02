package ru.yandex.practicum.client.stats;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.dto.EndpointHitDto;
import ru.yandex.practicum.dto.ViewStatsDto;

import java.util.List;

@FeignClient(name = "stats-service")
public interface StatsServiceClient {

    @PostMapping("/hit")
    void saveHit(@RequestBody EndpointHitDto hitDto);

    @GetMapping("/stats")
    List<ViewStatsDto> getStats(
            @RequestParam("start") String start,
            @RequestParam("end") String end,
            @RequestParam(value = "uris", required = false) List<String> uris,
            @RequestParam(value = "unique", defaultValue = "false") Boolean unique
    );
}
