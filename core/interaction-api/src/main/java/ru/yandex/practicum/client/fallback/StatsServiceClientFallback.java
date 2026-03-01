package ru.yandex.practicum.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.client.stats.StatsServiceClient;
import ru.yandex.practicum.dto.EndpointHitDto;
import ru.yandex.practicum.dto.ViewStatsDto;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class StatsServiceClientFallback implements StatsServiceClient {

    @Override
    public void saveHit(EndpointHitDto hitDto) {
        log.warn("Fallback: saveHit - сервис статистики недоступен, хит не сохранен: {}", hitDto.getUri());
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        log.warn("Fallback: getStats - сервис статистики недоступен, возвращаем пустой список");
        return Collections.emptyList();
    }
}