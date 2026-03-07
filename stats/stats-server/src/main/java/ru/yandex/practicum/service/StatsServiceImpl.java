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
        validateHit(dto);

        log.debug("Сохранение просмотра: {}", dto);
        EndpointHit entity = statsMapper.toEntity(dto);
        statsRepository.save(entity);

        log.info("Просмотр успешно сохранен с ID: {}", entity.getId());
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        log.info("Получение статистики: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

            LocalDateTime startTime = parseDateTime(start);
            LocalDateTime endTime = parseDateTime(end);

            validateDateRange(startTime, endTime);

            List<ViewStatsDto> stats = unique
                    ? statsRepository.findUniqueStats(startTime, endTime, uris)
                    : statsRepository.findAllStats(startTime, endTime, uris);

            log.info("Найдено записей: {}", stats.size());
            return stats;
    }

    private LocalDateTime parseDateTime(String dateTime) {
        try {
            return LocalDateTime.parse(dateTime, FORMATTER);
        } catch (Exception e) {
            log.error("Ошибка парсинга даты: {}", dateTime);
            throw new IllegalArgumentException("Неверный формат даты. Ожидается: yyyy-MM-dd HH:mm:ss", e);
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (end.isBefore(start)) {
            log.error("Некорректный диапазон: start={}, end={}", start, end);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Дата начала должна быть раньше даты окончания");
        }
    }

    private void validateHit(EndpointHitDto dto) {
        if (dto == null) {
            log.error("Попытка сохранения null-объекта");
            throw new IllegalArgumentException("DTO не может быть null");
        }
    }
}