package ru.yandex.practicum.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.client.event.EventServiceClient;
import ru.yandex.practicum.dto.event.EventFullDto;
import ru.yandex.practicum.exception.NotFoundException;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class EventServiceClientFallback implements EventServiceClient {

    @Override
    public EventFullDto getEvent(Long id) {
        log.error("Fallback: getEvent - сервис событий недоступен для eventId: {}", id);
        throw new NotFoundException("Сервис событий временно недоступен. Не удалось получить событие id=" + id);
    }

    @Override
    public List<EventFullDto> getEventsByIds(List<Long> ids) {
        log.warn("Fallback: getEventsByIds - сервис событий недоступен, возвращаем пустой список. ids: {}", ids);
        return Collections.emptyList();
    }
}