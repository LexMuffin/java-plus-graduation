package ru.yandex.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.starter.AggregationStarter;

@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan
public class AggregatorApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AggregatorApp.class, args);
        AggregationStarter aggregationStarter = context.getBean(AggregationStarter.class);

        Runtime.getRuntime().addShutdownHook(new Thread(aggregationStarter::stop));

        Thread aggregatorThread = new Thread(aggregationStarter::start);
        aggregatorThread.setName("AggregatorStarter");
        aggregatorThread.start();
    }
}
