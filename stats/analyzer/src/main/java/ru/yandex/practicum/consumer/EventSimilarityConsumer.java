package ru.yandex.practicum.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.service.EventSimilarityService;

import javax.annotation.PostConstruct;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityConsumer {

    private final KafkaConsumerConfig kafkaConfig;
    private final EventSimilarityService similarityService;

    @PostConstruct
    public void start() {
        new Thread(this::consume).start();
    }

    private void consume() {
        var consumer = kafkaConfig.getEventsSimilarityConsumer();
        consumer.subscribe(java.util.List.of(kafkaConfig.getEventsSimilarityTopic()));

        while (true) {
            try {
                ConsumerRecords<Long, EventSimilarityAvro> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<Long, EventSimilarityAvro> record : records) {
                    log.info("Получено сходство: eventA={}, eventB={}",
                            record.value().getEventA(), record.value().getEventB());
                    similarityService.processEventSimilarity(record.value());
                }
                consumer.commitSync();
            } catch (Exception e) {
                log.error("Ошибка обработки", e);
            }
        }
    }
}