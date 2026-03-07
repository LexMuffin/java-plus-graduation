package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.stats.EndpointHitDto;
import ru.yandex.practicum.dto.stats.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {
    void hit(EndpointHitDto dto);
    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
