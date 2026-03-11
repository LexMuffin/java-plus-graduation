package ru.yandex.practicum.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.service.UserActionService;

import javax.annotation.PostConstruct;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final KafkaConsumerConfig kafkaConfig;
    private final UserActionService userActionService;

    @PostConstruct
    public void start() {
        new Thread(this::consume).start();
    }

    private void consume() {
        var consumer = kafkaConfig.getUserActionsConsumer();
        consumer.subscribe(java.util.List.of(kafkaConfig.getUserActionsTopic()));

        while (true) {
            try {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    log.info("Получено действие: userId={}, eventId={}",
                            record.value().getUserId(), record.value().getEventId());
                    userActionService.processUserAction(record.value());
                }
                consumer.commitSync();
            } catch (Exception e) {
                log.error("Ошибка обработки", e);
            }
        }
    }
}