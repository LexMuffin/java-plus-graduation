package ru.yandex.practicum.service;

import ru.yandex.practicum.EndpointHitDto;
import ru.yandex.practicum.ViewStatsDto;

import java.util.List;

public interface StatsService {
    void hit(EndpointHitDto endpointHitDto);
    List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique);
}
