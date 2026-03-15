package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.model.UserEventInteraction;
import ru.yandex.practicum.repository.UserEventInteractionRepository;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService {

    private final UserEventInteractionRepository repository;

    @Override
    @Transactional
    public void processUserAction(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        double weight = getActionWeight(action);
        Instant timestamp = action.getTimestamp();

        log.info("Обработка действия пользователя: userId={}, eventId={}, тип={}, вес={}",
                userId, eventId, action.getActionType(), weight);

        repository.findByUserIdAndEventId(userId, eventId)
                .ifPresentOrElse(
                        existing -> updateIfWeightGreater(existing, weight, timestamp),
                        () -> createNew(userId, eventId, weight, timestamp)
                );
    }

    private void updateIfWeightGreater(UserEventInteraction existing,
                                       double newWeight,
                                       Instant timestamp) {
        if (newWeight > existing.getWeight()) {
            existing.setWeight(newWeight);
            existing.setTimestamp(timestamp);
            repository.save(existing);
            log.info("Обновлен вес для userId={}, eventId={}: {} -> {}",
                    existing.getUserId(), existing.getEventId(), existing.getWeight(), newWeight);
        } else {
            log.debug("Пропуск обновления: новый вес {} <= старый {}", newWeight, existing.getWeight());
        }
    }

    private void createNew(long userId, long eventId, double weight, Instant timestamp) {
        UserEventInteraction newInteraction = UserEventInteraction.builder()
                .userId(userId)
                .eventId(eventId)
                .weight(weight)
                .timestamp(timestamp)
                .build();

        repository.save(newInteraction);
        log.info("Создано новое взаимодействие: userId={}, eventId={}, вес={}",
                userId, eventId, weight);
    }

    private double getActionWeight(UserActionAvro action) {
        return switch (action.getActionType()) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}