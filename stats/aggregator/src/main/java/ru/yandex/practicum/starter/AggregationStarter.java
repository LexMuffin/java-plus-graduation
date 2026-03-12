package ru.yandex.practicum.starter;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.kafka.AggregatorKafkaProperties;
import ru.yandex.practicum.service.AggregatorService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AggregationStarter {

    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private final AggregatorService aggregatorService;
    private final KafkaProducer<Long, SpecificRecordBase> producer;
    private final KafkaConsumer<Long, UserActionAvro> consumer;
    private final AggregatorKafkaProperties kafkaConfig;

    public AggregationStarter(AggregatorService aggregatorService, AggregatorKafkaProperties kafkaConfig) {
        this.aggregatorService = aggregatorService;
        this.kafkaConfig = kafkaConfig;
        this.producer = new KafkaProducer<>(kafkaConfig.getProducerProps());
        this.consumer = new KafkaConsumer<>(kafkaConfig.getConsumerProps());
    }

    public void start() {
        log.info("Запуск сервиса aggregator");
        try {
            consumer.subscribe(List.of(kafkaConfig.getUserActionsTopic()));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            while (true) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(Duration.ofMillis(100));

                int count = 0;
                for (ConsumerRecord<Long, UserActionAvro> record : records) {

                    log.info("Получено сообщение: userId={}, eventId={}, offset={}",
                            record.value().getUserId(), record.value().getEventId(), record.offset());

                    List<EventSimilarityAvro> eventSimilarityAvros =
                            aggregatorService.updateSimilarity(record.value());

                    for (EventSimilarityAvro eventSimilarity : eventSimilarityAvros) {
                        ProducerRecord<Long, SpecificRecordBase> producerRecord = new ProducerRecord<>(
                                kafkaConfig.getEventsSimilarityTopic(),
                                null,
                                eventSimilarity.getTimestamp().toEpochMilli(),
                                eventSimilarity.getEventA(),
                                eventSimilarity);
                        producer.send(producerRecord);

                        log.info("Сходство для событий {} и {} отправлено",
                                eventSimilarity.getEventA(), eventSimilarity.getEventB());
                    }

                    manageOffsets(record, count, consumer);
                    count++;
                }
            }

        } catch (WakeupException ignored) {
            log.info("Consumer получил WakeupException, завершение работы");
        } catch (Exception e) {
            log.error("Ошибка обработки user actions", e);
        } finally {
            try {
                producer.flush();
                consumer.commitSync();
            } finally {
                log.info("Закрываем consumer");
                consumer.close();
                log.info("Закрываем producer");
                producer.close();
            }
        }
    }

    private void manageOffsets(ConsumerRecord<Long, UserActionAvro> record, int count,
                               KafkaConsumer<Long, UserActionAvro> consumer) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 5 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    public void stop() {
        log.info("Остановка агрегатора через wakeup...");
        consumer.wakeup();
    }
}