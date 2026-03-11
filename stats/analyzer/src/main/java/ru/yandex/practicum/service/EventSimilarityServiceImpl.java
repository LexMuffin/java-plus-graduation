package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.model.EventSimilarity;
import ru.yandex.practicum.repository.EventSimilarityRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityServiceImpl implements EventSimilarityService {

    private final EventSimilarityRepository repository;

    @Override
    @Transactional
    public void processEventSimilarity(EventSimilarityAvro avro) {
        EventSimilarity entity = repository.findByEventAAndEventB(avro.getEventA(), avro.getEventB())
                .map(existing -> updateExisting(existing, avro))
                .orElseGet(() -> createNew(avro));

        repository.save(entity);
        log.debug("Сходство сохранено: ({}, {}) = {}",
                entity.getEventA(), entity.getEventB(), entity.getScore());
    }

    private EventSimilarity updateExisting(EventSimilarity existing, EventSimilarityAvro avro) {
        existing.setScore(avro.getScore());
        return existing;
    }

    private EventSimilarity createNew(EventSimilarityAvro avro) {
        return new EventSimilarity(
                null,
                avro.getEventA(),
                avro.getEventB(),
                avro.getScore()
        );
    }
}
