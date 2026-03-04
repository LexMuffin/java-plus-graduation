package ru.yandex.practicum.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.dto.EndpointHitDto;
import ru.yandex.practicum.dto.ViewStatsDto;
import ru.yandex.practicum.mapper.StatsMapper;
import ru.yandex.practicum.model.EndpointHit;
import ru.yandex.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    StatsRepository statsRepository;
    StatsMapper statsMapper;

    @Override
    @Transactional
    public void hit(EndpointHitDto dto) {
        validateHit(dto);

        log.debug("Сохранение просмотра: {}", dto);
        EndpointHit entity = statsMapper.toEntity(dto);
        statsRepository.save(entity);

        log.info("Просмотр успешно сохранен с ID: {}", entity.getId());
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);

        validateDateRange(startTime, endTime);

        log.info("Запрос статистики: период {} - {}, uris: {}, unique: {}",
                startTime, endTime, uris, unique);

        List<ViewStatsDto> stats = unique
                ? statsRepository.findUniqueStats(startTime, endTime, uris)
                : statsRepository.findAllStats(startTime, endTime, uris);

        log.debug("Найдено записей: {}", stats.size());
        return stats;
    }

    private void validateHit(EndpointHitDto dto) {
        if (dto == null) {
            log.error("Попытка сохранения null-объекта");
            throw new IllegalArgumentException("DTO не может быть null");
        }

        if (dto.getApp() == null || dto.getApp().isBlank()) {
            throw new IllegalArgumentException("app не может быть пустым");
        }
        if (dto.getUri() == null || dto.getUri().isBlank()) {
            throw new IllegalArgumentException("uri не может быть пустым");
        }
        if (dto.getIp() == null || dto.getIp().isBlank()) {
            throw new IllegalArgumentException("ip не может быть пустым");
        }
        if (dto.getTimestamp() == null) {
            throw new IllegalArgumentException("timestamp не может быть null");
        }
    }

    private LocalDateTime parseDateTime(String dateTime) {
        try {
            String decoded = java.net.URLDecoder.decode(dateTime, "UTF-8");
            return LocalDateTime.parse(decoded, FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTime, FORMATTER);
            } catch (Exception ex) {
                log.error("Ошибка парсинга даты: {}", dateTime);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Неверный формат даты. Ожидается: yyyy-MM-dd HH:mm:ss");
            }
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Даты начала и окончания должны быть указаны");
        }
        if (end.isBefore(start)) {
            log.error("Некорректный диапазон: start={}, end={}", start, end);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Дата начала должна быть раньше даты окончания");
        }
    }
}