package ru.yandex.practicum.kafka;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Slf4j
@Getter
@Configuration
@EnableConfigurationProperties({CollectorKafkaProperties.class})
public class KafkaConfig {

    private final CollectorKafkaProperties collectorKafkaProperties;

    public KafkaConfig(CollectorKafkaProperties properties) {
        this.collectorKafkaProperties = properties;
        log.info("KafkaConfig created with properties:");
        log.info("  bootstrapServers = {}", properties.getBootstrapServers());
        log.info("  clientIdConfig = {}", properties.getClientIdConfig());
        log.info("  userActionTopic = {}", properties.getUserActionTopic());
    }

    @Bean
    public Producer<Long, SpecificRecordBase> producer() {
        log.info("Creating Kafka producer with bootstrap servers: {}",
                collectorKafkaProperties.getBootstrapServers());

        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, collectorKafkaProperties.getBootstrapServers());
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, collectorKafkaProperties.getClientIdConfig());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, collectorKafkaProperties.getProducerKeySerializer());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, collectorKafkaProperties.getProducerValueSerializer());

        log.info("Producer config: {}", properties);

        try {
            return new KafkaProducer<>(properties);
        } catch (Exception e) {
            log.error("Failed to create Kafka producer", e);
            throw e;
        }
    }
}