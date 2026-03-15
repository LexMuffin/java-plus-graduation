package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.yandex.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorService {

    private final Map<Long, Map<Long, Double>> weightsStorage = new ConcurrentHashMap<>();

    private final Map<Long, Double> sumsStorage = new ConcurrentHashMap<>();

    private final Map<String, Double> minsStorage = new ConcurrentHashMap<>();

    public List<EventSimilarityAvro> updateSimilarity(UserActionAvro action) {
        Long userId = action.getUserId();
        Long currentEvent = action.getEventId();
        Double currentWeight = resolveWeight(action.getActionType());

        Map<Long, Double> userEventWeights = weightsStorage.computeIfAbsent(currentEvent,
                k -> new ConcurrentHashMap<>());

        Double previousWeight = userEventWeights.getOrDefault(userId, 0.0);

        if (previousWeight >= currentWeight) {
            return Collections.emptyList();
        }

        userEventWeights.put(userId, currentWeight);
        updateSumsStorage(currentEvent, previousWeight, currentWeight);

        List<EventSimilarityAvro> results = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, Double>> entry : weightsStorage.entrySet()) {
            Long otherEvent = entry.getKey();

            if (otherEvent.equals(currentEvent)) {
                continue;
            }

            Map<Long, Double> otherWeights = entry.getValue();
            if (!otherWeights.containsKey(userId)) {
                continue;
            }

            Double otherWeight = otherWeights.get(userId);
            Double pairMinSum = updatePairMinSum(currentEvent, otherEvent, userId,
                    previousWeight, currentWeight, otherWeight);

            Double similarity = calculatePairSimilarity(currentEvent, otherEvent, pairMinSum);

            results.add(buildSimilarityRecord(currentEvent, otherEvent, similarity, action.getTimestamp()));
        }

        return results;
    }

    private void updateSumsStorage(Long eventId, Double oldWeight, Double newWeight) {
        sumsStorage.merge(eventId, newWeight - oldWeight, Double::sum);
    }

    private Double updatePairMinSum(Long eventA, Long eventB, Long userId,
                                    Double oldWeightA, Double newWeightA, Double weightB) {
        String pairKey = buildPairKey(eventA, eventB);

        Double oldMin = Math.min(oldWeightA, weightB);
        Double newMin = Math.min(newWeightA, weightB);

        if (oldMin.equals(newMin)) {
            return minsStorage.getOrDefault(pairKey, 0.0);
        }

        Double currentSum = minsStorage.getOrDefault(pairKey, 0.0);
        Double updatedSum = currentSum - oldMin + newMin;
        minsStorage.put(pairKey, updatedSum);

        return updatedSum;
    }

    private Double calculatePairSimilarity(Long eventA, Long eventB, Double minSum) {
        if (minSum == 0.0) {
            return 0.0;
        }

        Double sumA = sumsStorage.getOrDefault(eventA, 0.0);
        Double sumB = sumsStorage.getOrDefault(eventB, 0.0);

        if (sumA == 0.0 || sumB == 0.0) {
            return 0.0;
        }

        return minSum / (Math.sqrt(sumA) * Math.sqrt(sumB));
    }

    private String buildPairKey(Long id1, Long id2) {
        return id1 < id2 ? id1 + "_" + id2 : id2 + "_" + id1;
    }

    private Double resolveWeight(ActionTypeAvro actionType) {
        if (actionType == ActionTypeAvro.VIEW) return 0.4;
        if (actionType == ActionTypeAvro.REGISTER) return 0.8;
        return 1.0;
    }

    private EventSimilarityAvro buildSimilarityRecord(Long eventA, Long eventB,
                                                      Double score, Instant timestamp) {
        Long smaller = Math.min(eventA, eventB);
        Long larger = Math.max(eventA, eventB);

        return EventSimilarityAvro.newBuilder()
                .setEventA(smaller)
                .setEventB(larger)
                .setScore(score)
                .setTimestamp(timestamp)
                .build();
    }
}