package ru.yandex.practicum.request;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.yandex.practicum.client")
@ComponentScan(basePackages = {
        "ru.yandex.practicum.request",
        "ru.yandex.practicum.client",
        "ru.yandex.practicum.exception"
})
public class RequestServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ru.yandex.practicum.request.RequestServiceApplication.class, args);
    }
}