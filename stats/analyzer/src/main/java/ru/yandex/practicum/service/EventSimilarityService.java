package ru.yandex.practicum.service;


import ru.yandex.practicum.ewm.stats.avro.EventSimilarityAvro;

public interface EventSimilarityService {

    void processEventSimilarity(EventSimilarityAvro similarity);
}
