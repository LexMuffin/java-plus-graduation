package ru.yandex.practicum.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.ewm.grpc.stats.messages.UserActionProto;
import ru.yandex.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.yandex.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.kafka.KafkaConfig;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActionService {

    private final Producer<Long, SpecificRecordBase> producer;
    private final KafkaConfig kafkaConfig;

    public void collectUserAction(UserActionProto userActionProto) {
        log.info("UserActionService: обработка UserActionProto, userId={}, eventId={}",
                userActionProto.getUserId(), userActionProto.getEventId());

        try {

            UserActionAvro userActionAvro = UserActionAvro.newBuilder()
                    .setUserId(userActionProto.getUserId())
                    .setEventId(userActionProto.getEventId())
                    .setActionType(toActionTypeAvro(userActionProto))
                    .setTimestamp(Instant.ofEpochSecond(
                            userActionProto.getTimestamp().getSeconds(),
                            userActionProto.getTimestamp().getNanos()
                    )
                    )
                    .build();

            send(kafkaConfig.getCollectorKafkaProperties().getUserActionTopic(),
                    userActionAvro.getEventId(),
                    userActionAvro.getTimestamp().toEpochMilli(),
                    userActionAvro);

            log.info("UserAction успешно отправлен в Kafka: userId={}, eventId={}, timestamp={}",
                    userActionProto.getUserId(), userActionProto.getEventId(), userActionAvro.getTimestamp().toEpochMilli());

        } catch (IllegalArgumentException e) {
            log.error("Невалидное действие пользователя: userId={}, eventId={}, actionType={}",
                    userActionProto.getUserId(),
                    userActionProto.getEventId(),
                    userActionProto.getActionType(), e);
        } catch (Exception e) {
            log.error("Ошибка при обработке действия пользователя: userId={}, eventId={}",
                    userActionProto.getUserId(), userActionProto.getEventId(), e);
        }
    }

    private ActionTypeAvro toActionTypeAvro(UserActionProto userActionProto) {
        return switch (userActionProto.getActionType()) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException(
                    String.format("Неизвестный тип действия: %s (userId=%d, eventId=%d)",
                            userActionProto.getActionType(),
                            userActionProto.getUserId(),
                            userActionProto.getEventId())
            );
        };
    }

    private void send(String topic, Long key, Long timestamp, SpecificRecordBase specificRecordBase) {
        ProducerRecord<Long, SpecificRecordBase> rec = new ProducerRecord<>(
                topic,
                null,
                timestamp,
                key,
                specificRecordBase);

        producer.send(rec, (metadata, exception) -> {
            if (exception != null) {
                log.error("Kafka: сообщение не отправлено, topic: {}, key: {}", topic, key, exception);
            } else {
                log.info("Kafka: сообщение отправлено, topic: {}, partition: {}, offset: {}, timestamp: {}",
                        metadata.topic(), metadata.partition(), metadata.offset(), timestamp);
            }
        });
    }

    @PreDestroy
    private void close() {
        if (producer != null) {
            log.info("Закрытие Kafka producer");
            producer.flush();
            producer.close();
        }
    }
}