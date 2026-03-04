package ru.yandex.practicum;

import ru.yandex.practicum.dto.stats.EndpointHitDto;
import ru.yandex.practicum.dto.stats.ViewStatsDto;

import java.util.List;

public interface StatsClient {
    void saveHit(EndpointHitDto endpointHitDto);
    List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique);
}
