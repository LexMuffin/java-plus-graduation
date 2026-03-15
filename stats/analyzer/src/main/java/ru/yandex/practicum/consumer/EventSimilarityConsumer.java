package ru.yandex.practicum.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.service.EventSimilarityService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class EventSimilarityConsumer implements Runnable {
    private final KafkaConsumer<Long, EventSimilarityAvro> consumer;
    private final EventSimilarityService similarityService;
    private final KafkaConsumerConfig kafkaConfig;
    private Thread consumerThread;

    public EventSimilarityConsumer(KafkaConsumerConfig kafkaConfig, EventSimilarityService similarityService) {
        this.kafkaConfig = kafkaConfig;
        this.similarityService = similarityService;
        this.consumer = new KafkaConsumer<>(kafkaConfig.getEventSimilarityConsumerProps());
    }

    @PostConstruct
    public void start() {
        consumerThread = new Thread(this, "EventSimilarityConsumer");
        consumerThread.start();
        log.info("EventSimilarityConsumer запущен");
    }

    @Override
    public void run() {
        log.info("Запуск потока EventSimilarityConsumer");
        try {
            consumer.subscribe(List.of(kafkaConfig.getEventsSimilarityTopic()));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            while (true) {
                try {
                    ConsumerRecords<Long, EventSimilarityAvro> records = consumer.poll(Duration.ofMillis(500));
                    if (records.isEmpty()) continue;

                    for (ConsumerRecord<Long, EventSimilarityAvro> record : records) {
                        log.info("Получено сходство: eventA={}, eventB={}, score={}",
                                record.value().getEventA(),
                                record.value().getEventB(),
                                record.value().getScore());

                        similarityService.processEventSimilarity(record.value());
                    }

                    consumer.commitAsync((offsets, exception) -> {
                        if (exception != null) {
                            log.warn("Ошибка фиксации оффсетов", exception);
                        }
                    });

                } catch (WakeupException e) {
                    log.info("EventSimilarityProcessor получил WakeupException, завершение работы");
                    break;
                } catch (Exception e) {
                    log.error("Ошибка обработки сообщения", e);
                }
            }

        } catch (Exception e) {
            log.error("Фатальная ошибка в EventSimilarityConsumer", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Закрываем consumer");
                consumer.close();
            }
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Остановка EventSimilarityConsumer");
        consumer.wakeup();
        try {
            if (consumerThread != null) {
                consumerThread.join(5000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Прерывание при ожидании завершения потока", e);
        }
    }
}