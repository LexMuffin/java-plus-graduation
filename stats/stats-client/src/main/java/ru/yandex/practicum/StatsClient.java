package ru.yandex.practicum;

import java.util.List;

public interface StatsClient {
    void saveHit(EndpointHitDto endpointHitDto);
    List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique);
}
