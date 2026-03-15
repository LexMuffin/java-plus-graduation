package ru.yandex.practicum.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.service.UserActionService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class UserActionConsumer implements Runnable {
    private final KafkaConsumer<Long, UserActionAvro> consumer;
    private final UserActionService userActionService;
    private final KafkaConsumerConfig kafkaConfig;
    private Thread consumerThread;

    public UserActionConsumer(KafkaConsumerConfig kafkaConfig, UserActionService userActionService) {
        this.kafkaConfig = kafkaConfig;
        this.userActionService = userActionService;
        this.consumer = new KafkaConsumer<>(kafkaConfig.getUserActionConsumerProps());
    }

    @PostConstruct
    public void start() {
        kafkaConfig.logConfig();
        consumerThread = new Thread(this, "UserActionConsumer");
        consumerThread.start();
        log.info("UserActionConsumer запущен");
    }

    @Override
    public void run() {
        log.info("Запуск потока UserActionConsumer");
        try {
            consumer.subscribe(List.of(kafkaConfig.getUserActionsTopic()));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            while (true) {
                try {
                    ConsumerRecords<Long, UserActionAvro> records = consumer.poll(Duration.ofMillis(500));
                    if (records.isEmpty()) continue;

                    for (ConsumerRecord<Long, UserActionAvro> record : records) {
                        log.info("Получено действие: userId={}, eventId={}",
                                record.value().getUserId(), record.value().getEventId());

                        userActionService.processUserAction(record.value());
                    }

                    consumer.commitAsync((offsets, exception) -> {
                        if (exception != null) {
                            log.warn("Ошибка фиксации оффсетов", exception);
                        }
                    });

                } catch (WakeupException e) {
                    log.info("UserActionConsumer получил WakeupException, завершение работы");
                    break;
                } catch (Exception e) {
                    log.error("Ошибка обработки сообщения", e);
                }
            }

        } catch (Exception e) {
            log.error("Фатальная ошибка в UserActionConsumer", e);
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
        log.info("Остановка UserActionConsumer");
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