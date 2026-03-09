package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.dto.stats.EndpointHitDto;
import ru.yandex.practicum.dto.stats.ViewStatsDto;
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

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;

    @Value("${stats-server.id:stats-service}")
    private String statServiceId;

    public StatsClientImpl(RestTemplate restTemplate, DiscoveryClient discoveryClient) {
        this.restTemplate = restTemplate;
        this.discoveryClient = discoveryClient;
    }

    private String getServiceUrl() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(statServiceId);
            if (instances == null || instances.isEmpty()) {
                throw new StatsServerUnavailableException(
                        "Сервис статистики с id '" + statServiceId + "' не найден"
                );
            }
            ServiceInstance instance = instances.get(0);
            String url = instance.getUri().toString();
            log.debug("Получен URL сервиса статистики: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Ошибка получения URL сервиса статистики: {}", e.getMessage());
            throw new StatsServerUnavailableException(
                    "Не удалось получить URL сервиса статистики: " + e.getMessage()
            );
        }
    }

    @Override
    public void saveHit(EndpointHitDto hitDto) {
        log.info("Сохранение хита: {}", hitDto);

        try {
            String url = getServiceUrl() + "/hit";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<EndpointHitDto> requestEntity = new HttpEntity<>(hitDto, headers);

            ResponseEntity<Void> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    Void.class
            );

            if (response.getStatusCode() != HttpStatus.CREATED) {
                throw new InvalidRequestException(
                        "Ошибка при сохранении хита: " + response.getStatusCode().value()
                );
            }

            log.info("Хит успешно сохранен");
        } catch (RestClientException e) {
            log.error("Ошибка при сохранении хита: {}", e.getMessage());
            throw new StatsServerUnavailableException("Сервис статистики временно недоступен: " + e.getMessage());
        } catch (InvalidRequestException e) {
            log.error("Ошибка запроса: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при сохранении хита: {}", e.getMessage());
            throw new StatsServerUnavailableException("Неожиданная ошибка при сохранении хита");
        }
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        validateDates(start, end);

        log.info("Запрос статистики: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

        try {
            String url = buildStatsUrl(start, end, uris, unique);

            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStatsDto>>() {}
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new InvalidRequestException(
                        "Ошибка при получении статистики: " + response.getStatusCode().value()
                );
            }

            List<ViewStatsDto> stats = response.getBody();
            if (stats == null) {
                log.info("Статистика не найдена, возвращаем пустой список");
                return new ArrayList<>();
            }

            log.info("Получено записей статистики: {}", stats.size());
            return stats;

        } catch (RestClientException e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage());
            throw new StatsServerUnavailableException("Сервис статистики временно недоступен: " + e.getMessage());
        } catch (InvalidRequestException e) {
            log.error("Ошибка запроса статистики: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String buildStatsUrl(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        String baseUrl = getServiceUrl();

        String startStr = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String endStr = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder url = new StringBuilder(baseUrl + "/stats?start=" + startStr + "&end=" + endStr);

        if (uris != null && !uris.isEmpty()) {
            uris.stream()
                    .forEach(uri -> url.append("&uris=").append(uri));
        }

        if (Boolean.TRUE.equals(unique)) {
            url.append("&unique=true");
        }

        return url.toString();
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (end.isBefore(start)) {
            log.error("Некорректный диапазон: start={}, end={}", start, end);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Дата начала должна быть раньше даты окончания");
        }
    }
}