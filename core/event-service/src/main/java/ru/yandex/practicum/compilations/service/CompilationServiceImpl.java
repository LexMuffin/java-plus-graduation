package ru.yandex.practicum.compilations.service;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.event.EventServiceClient;
import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.dto.compilation.NewCompilationDto;
import ru.yandex.practicum.dto.compilation.UpdateCompilationRequest;
import ru.yandex.practicum.dto.event.EventFullDto;
import ru.yandex.practicum.compilations.mapper.CompilationMapper;
import ru.yandex.practicum.compilations.model.Compilation;
import ru.yandex.practicum.compilations.repository.CompilationRepository;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.event.repository.EventRepository;
import ru.yandex.practicum.exception.ConflictException;
import ru.yandex.practicum.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    CompilationRepository compilationRepository;
    EventRepository eventRepository;
    CompilationMapper compilationMapper;
    EventServiceClient eventClient;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto dto) {
        log.info("Создание подборки: {}", dto.getTitle());

        Compilation compilation = compilationMapper.toEntity(dto);

        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            List<EventFullDto> eventDtos = eventClient.getEventsByIds(dto.getEvents());
            List<Long> existingIds = eventDtos.stream()
                    .map(EventFullDto::getId)
                    .collect(Collectors.toList());
            List<Event> events = eventRepository.findAllById(existingIds);
            compilation.setEvents(events);
        } else {
            compilation.setEvents(new ArrayList<>());
        }

        if (compilation.getPinned() == null) {
            compilation.setPinned(false);
        }

        try {
            Compilation saved = compilationRepository.save(compilation);
            log.info("Подборка создана, id: {}", saved.getId());
            return compilationMapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            log.error("Название уже существует: {}", dto.getTitle());
            throw new ConflictException("Подборка с таким названием уже существует");
        }
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки: {}", compId);

        if (!compilationRepository.existsById(compId)) {
            log.error("Подборка не найдена: {}", compId);
            throw new NotFoundException("Подборка не найдена");
        }

        compilationRepository.deleteById(compId);
        log.info("Подборка удалена: {}", compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        log.info("Обновление подборки: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> {
                    log.error("Подборка не найдена: {}", compId);
                    return new NotFoundException("Подборка не найдена");
                });

        List<Event> events = null;
        if (request.getEvents() != null) {
            List<EventFullDto> eventDtos = eventClient.getEventsByIds(request.getEvents());
            List<Long> existingIds = eventDtos.stream()
                    .map(EventFullDto::getId)
                    .collect(Collectors.toList());
            events = eventRepository.findAllById(existingIds);
        }

        compilationMapper.updateEntity(request, compilation, events);

        try {
            Compilation updated = compilationRepository.save(compilation);
            log.info("Подборка обновлена, id: {}", updated.getId());
            return compilationMapper.toDto(updated);
        } catch (DataIntegrityViolationException e) {
            log.error("Название уже существует: {}", request.getTitle());
            throw new ConflictException("Подборка с таким названием уже существует");
        }
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Pageable pageable) {
        log.info("Запрос подборок: pinned={}, page={}", pinned, pageable.getPageNumber());

        if (pinned == null) {
            return compilationMapper.toDto(compilationRepository.findAll(pageable).getContent());
        } else {
            return compilationMapper.toDto(compilationRepository.findByPinned(pinned, pageable).getContent());
        }
    }

    @Override
    public CompilationDto getCompilation(Long compId) {
        log.info("Запрос подборки: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> {
                    log.error("Подборка не найдена: {}", compId);
                    return new NotFoundException("Подборка не найдена");
                });

        return compilationMapper.toDto(compilation);
    }
}