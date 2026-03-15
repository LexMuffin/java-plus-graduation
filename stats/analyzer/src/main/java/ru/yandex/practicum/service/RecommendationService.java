package ru.yandex.practicum.service;

import ru.yandex.practicum.ewm.grpc.stats.messages.RecommendedEventProto;

import java.util.List;

public interface RecommendationService {

    List<RecommendedEventProto> getRecommendationsForUser(Long userId, Integer maxResults);

    List<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, Integer maxResults);

    List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds);

    Double predictScore(Long userId, Long targetEventId);
}
