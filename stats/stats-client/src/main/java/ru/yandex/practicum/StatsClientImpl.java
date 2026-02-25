package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class StatsClientImpl implements StatsClient {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestClient restClient;

    public StatsClientImpl(@Value("${stats-server.url}") String statsServerUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(statsServerUrl)
                .defaultStatusHandler(HttpStatusCode::isError, this::handleError)
                .build();
    }

    private void handleError(HttpRequest request, ClientHttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();
        String statusText = response.getStatusText();

        String errorMessage = String.format(
                "Ошибка при обращении к серверу статистики: %d %s для URI: %s",
                statusCode.value(),
                statusText,
                request.getURI()
        );

        log.error(errorMessage);

        if (statusCode.is4xxClientError()) {
            throw new RestClientException("Ошибка запроса клиента: " + errorMessage);
        } else if (statusCode.is5xxServerError()) {
            throw new RestClientException("Ошибка на сервере статистики: " + errorMessage);
        } else {
            throw new RestClientException("Неизвестная ошибка: " + errorMessage);
        }
    }

    @Override
    public void saveHit(EndpointHitDto hit) {
        log.info("Сохранение хита: {}", hit);

        restClient.post()
                .uri("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(hit)
                .retrieve()
                .toBodilessEntity();

        log.info("Хит успешно сохранен");
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        validateDates(start, end);

        log.info("Запрос статистики:");
        log.info("  start  = {}", start);
        log.info("  end    = {}", end);
        log.info("  uris   = {}", uris);
        log.info("  unique = {}", unique);

        String uri = buildStatsUri(start, end, uris, unique);

        List<ViewStatsDto> stats = restClient.get()
                .uri(uri)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        int size = stats != null ? stats.size() : 0;
        log.info("Получено записей статистики: {}", size);
        return stats;
    }

    private String buildStatsUri(String start, String end, List<String> uris, Boolean unique) {
        StringBuilder uri = new StringBuilder("/stats?start=" + start + "&end=" + end);

        if (uris != null && !uris.isEmpty()) {
            for (String u : uris) {
                uri.append("&uris=").append(u);
            }
        }

        if (unique != null) {
            uri.append("&unique=").append(unique);
        }

        return uri.toString();
    }

    private void validateDates(String start, String end) {
        try {
            LocalDateTime.parse(start, DATE_FORMATTER);
            LocalDateTime.parse(end, DATE_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Неверный формат даты. Ожидается: yyyy-MM-dd HH:mm:ss", e);
        }
    }
}