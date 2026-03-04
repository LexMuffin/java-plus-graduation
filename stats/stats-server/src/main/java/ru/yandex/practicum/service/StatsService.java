package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.stats.EndpointHitDto;
import ru.yandex.practicum.dto.stats.ViewStatsDto;

import java.util.List;

public interface StatsService {
    void hit(EndpointHitDto dto);
    List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique);
}
