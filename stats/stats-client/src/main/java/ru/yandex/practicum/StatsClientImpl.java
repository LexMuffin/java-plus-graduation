package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.dto.EndpointHitDto;
import ru.yandex.practicum.dto.ViewStatsDto;
import ru.yandex.practicum.exception.StatsServerUnavailableException;
import ru.yandex.practicum.exception.InvalidRequestException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class StatsClientImpl implements StatsClient {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DiscoveryClient discoveryClient;
    private RestClient restClient;

    @Value("${stats-server.id:stats-server}")
    private String statServiceId;

    public StatsClientImpl(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public void saveHit(EndpointHitDto hitDto) {
        log.info("Сохранение хита: {}", hitDto);

        try {
            getRestClient().post()
                    .uri("/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hitDto)
                    .retrieve()
                    .onStatus(status -> status != HttpStatus.CREATED, (request, response) -> {
                        throw new InvalidRequestException(
                                "Ошибка при сохранении хита: " + response.getStatusCode().value()
                        );
                    })
                    .toBodilessEntity();

            log.info("Хит успешно сохранен");
        } catch (InvalidRequestException e) {
            log.error("Ошибка запроса: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при сохранении хита: {}", e.getMessage());
        }
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        validateDates(start, end);

        log.info("Запрос статистики: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

        try {
            List<ViewStatsDto> stats = getRestClient().get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/stats")
                                .queryParam("start", start)
                                .queryParam("end", end);

                        if (uris != null && !uris.isEmpty()) {
                            uriBuilder.queryParam("uris", uris.toArray());
                        }

                        if (Boolean.TRUE.equals(unique)) {
                            uriBuilder.queryParam("unique", true);
                        }

                        return uriBuilder.build();
                    })
                    .retrieve()
                    .onStatus(status -> status != HttpStatus.OK, (request, response) -> {
                        throw new InvalidRequestException(
                                "Ошибка при получении статистики: " + response.getStatusCode().value()
                        );
                    })
                    .body(new ParameterizedTypeReference<List<ViewStatsDto>>() {});

            if (stats == null) {
                log.info("Статистика не найдена, возвращаем пустой список");
                return new ArrayList<>();
            }

            log.info("Получено записей статистики: {}", stats.size());
            return stats;

        } catch (InvalidRequestException e) {
            log.error("Ошибка запроса статистики: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private ServiceInstance getInstance() {
        try {
            ServiceInstance serviceInstance = discoveryClient.getInstances(statServiceId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new StatsServerUnavailableException(
                            "Сервис статистики не найден: " + statServiceId
                    ));

            log.info("Получен URI сервиса статистики: {}", serviceInstance.getUri());
            return serviceInstance;
        } catch (Exception e) {
            log.error("Ошибка обнаружения сервиса статистики: {}", e.getMessage());
            throw new StatsServerUnavailableException(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statServiceId
            );
        }
    }

    private RestClient getRestClient() {
        if (restClient == null) {
            ServiceInstance instance = getInstance();
            this.restClient = RestClient.create(instance.getUri().toString());
            log.info("RestClient создан с baseUrl: {}", instance.getUri());
        }
        return restClient;
    }

    private void validateDates(String start, String end) {
        try {
            LocalDateTime.parse(start, DATE_FORMATTER);
            LocalDateTime.parse(end, DATE_FORMATTER);
        } catch (Exception e) {
            throw new InvalidRequestException(
                    "Неверный формат даты. Ожидается: yyyy-MM-dd HH:mm:ss"
            );
        }
    }
}