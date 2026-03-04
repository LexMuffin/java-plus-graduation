package ru.yandex.practicum.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.dto.stats.EndpointHitDto;
import ru.yandex.practicum.dto.stats.ViewStatsDto;
import ru.yandex.practicum.mapper.StatsMapper;
import ru.yandex.practicum.model.EndpointHit;
import ru.yandex.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatsServiceImpl implements StatsService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    StatsRepository statsRepository;
    StatsMapper statsMapper;

    @Override
    @Transactional
    public void hit(EndpointHitDto dto) {
        log.info("Сохранение хита: {}", dto);

        try {
            if (dto == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DTO не может быть null");
            }

            EndpointHit entity = statsMapper.toEntity(dto);

            if (entity.getTimestamp() == null) {
                entity.setTimestamp(LocalDateTime.now());
            }

            EndpointHit saved = statsRepository.save(entity);
            log.info("Хит сохранен с ID: {}", saved.getId());

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при сохранении хита: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        log.info("Получение статистики: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        try {
            LocalDateTime startTime = parseDateTime(start);
            LocalDateTime endTime = parseDateTime(end);

            validateDateRange(startTime, endTime);

            List<ViewStatsDto> stats;
            if (unique) {
                stats = statsRepository.findUniqueStats(startTime, endTime, uris);
            } else {
                stats = statsRepository.findAllStats(startTime, endTime, uris);
            }

            log.info("Найдено записей: {}", stats.size());
            return stats;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    private LocalDateTime parseDateTime(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Дата не может быть пустой");
        }

        try {
            String decoded = java.net.URLDecoder.decode(dateTime, "UTF-8");
            return LocalDateTime.parse(decoded, FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTime, FORMATTER);
            } catch (Exception ex) {
                log.error("Ошибка парсинга даты: {}", dateTime);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Неверный формат даты. Ожидается: yyyy-MM-dd HH:mm:ss, получено: " + dateTime);
            }
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Даты начала и окончания должны быть указаны");
        }
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Дата начала должна быть раньше даты окончания");
        }
    }
}