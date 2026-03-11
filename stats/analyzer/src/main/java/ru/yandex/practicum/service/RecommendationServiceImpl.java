package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.ewm.grpc.stats.messages.RecommendedEventProto;
import ru.yandex.practicum.model.EventSimilarity;
import ru.yandex.practicum.model.UserEventInteraction;
import ru.yandex.practicum.repository.EventSimilarityRepository;
import ru.yandex.practicum.repository.UserEventInteractionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final UserEventInteractionRepository interactionRepository;
    private final EventSimilarityRepository similarityRepository;

    private static final int RECENT_INTERACTIONS_LIMIT = 20;
    private static final int NEIGHBORS_COUNT = 10;

    @Override
    @Transactional(readOnly = true)
    public List<RecommendedEventProto> getRecommendationsForUser(Long userId, Integer maxResults) {
        log.info("Генерация рекомендаций для пользователя {}", userId);

        List<UserEventInteraction> recentInteractions = interactionRepository
                .findByUserIdOrderByTimestampDesc(userId, PageRequest.of(0, RECENT_INTERACTIONS_LIMIT));

        if (recentInteractions.isEmpty()) {
            log.info("У пользователя {} нет взаимодействий", userId);
            return Collections.emptyList();
        }

        Set<Long> interactedEvents = recentInteractions.stream()
                .map(UserEventInteraction::getEventId)
                .collect(Collectors.toSet());

        Map<Long, Double> candidateScores = recentInteractions.stream()
                .flatMap(interaction -> similarityRepository.findAllByEventId(interaction.getEventId()).stream()
                        .map(sim -> Map.entry(
                                getPairEventId(sim, interaction.getEventId()),
                                sim.getScore()
                        )))
                .filter(entry -> !interactedEvents.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Double::sum
                ));

        return candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> buildRecommendedEvent(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, Integer maxResults) {
        log.info("Поиск похожих на мероприятие {} для пользователя {}", eventId, userId);

        return similarityRepository.findSimilarEvents(eventId, PageRequest.of(0, maxResults * 2))
                .stream()
                .filter(sim -> {
                    Long candidateId = getPairEventId(sim, eventId);
                    return !interactionRepository.existsByUserIdAndEventId(userId, candidateId);
                })
                .limit(maxResults)
                .map(sim -> buildRecommendedEvent(
                        getPairEventId(sim, eventId),
                        sim.getScore()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        log.info("Подсчет взаимодействий для {} мероприятий", eventIds.size());

        return interactionRepository.sumWeightsByEventIds(eventIds)
                .stream()
                .map(row -> buildRecommendedEvent((Long) row[0], (Double) row[1]))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Double predictScore(Long userId, Long targetEventId) {
        log.info("Предсказание оценки для пользователя {} и мероприятия {}", userId, targetEventId);

        List<UserEventInteraction> userInteractions = interactionRepository
                .findByUserIdOrderByTimestampDesc(userId, PageRequest.of(0, NEIGHBORS_COUNT));

        if (userInteractions.isEmpty()) {
            return 0.0;
        }

        List<Long> interactedEventIds = userInteractions.stream()
                .map(UserEventInteraction::getEventId)
                .collect(Collectors.toList());

        Map<Long, Double> similarityMap = similarityRepository
                .findSimilaritiesBetween(targetEventId, interactedEventIds)
                .stream()
                .collect(Collectors.toMap(
                        sim -> getPairEventId(sim, targetEventId),
                        EventSimilarity::getScore
                ));

        return userInteractions.stream()
                .filter(interaction -> {
                    Double similarity = similarityMap.get(interaction.getEventId());
                    return similarity != null && similarity > 0;
                })
                .mapToDouble(interaction -> {
                    Double similarity = similarityMap.get(interaction.getEventId());
                    return similarity * interaction.getWeight();
                })
                .sum() / similarityMap.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private Long getPairEventId(EventSimilarity similarity, Long sourceEventId) {
        return similarity.getEventA().equals(sourceEventId)
                ? similarity.getEventB()
                : similarity.getEventA();
    }

    private RecommendedEventProto buildRecommendedEvent(Long eventId, Double score) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(score)
                .build();
    }
}