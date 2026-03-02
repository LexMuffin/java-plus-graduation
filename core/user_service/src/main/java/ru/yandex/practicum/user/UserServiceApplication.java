package ru.yandex.practicum.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"ru.yandex.practicum.user", "ru.yandex.practicum.exception"})
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ru.yandex.practicum.user.UserServiceApplication.class, args);
    }
}
