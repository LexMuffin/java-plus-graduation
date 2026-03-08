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
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatsServiceImpl implements StatsService {

    StatsRepository statsRepository;
    StatsMapper statsMapper;

    @Override
    @Transactional
    public void hit(EndpointHitDto dto) {
        try {
            System.out.println("🔍 [STATS-SERVER] Получен хит: " + dto);
            EndpointHit entity = statsMapper.toEntity(dto);
            EndpointHit saved = statsRepository.save(entity);
            System.out.println("✅ [STATS-SERVER] Хит сохранен с ID: " + saved.getId());
        } catch (Exception e) {
            System.err.println("❌ [STATS-SERVER] Ошибка сохранения хита: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, boolean unique) {
        System.out.println("🔍 [STATS-SERVER] Запрос статистики: start=" + start +
                ", end=" + end + ", uris=" + uris + ", unique=" + unique);

        List<ViewStatsDto> stats = unique
                ? statsRepository.findUniqueStats(start, end, uris)
                : statsRepository.findAllStats(start, end, uris);

        System.out.println("✅ [STATS-SERVER] Найдено записей: " + stats.size());
        return stats;
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