package ru.yandex.practicum.compilations.service;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.compilations.dto.CompilationDto;
import ru.yandex.practicum.compilations.dto.NewCompilationDto;
import ru.yandex.practicum.compilations.dto.UpdateCompilationRequest;
import ru.yandex.practicum.compilations.mapper.CompilationMapper;
import ru.yandex.practicum.compilations.model.Compilation;
import ru.yandex.practicum.compilations.repository.CompilationRepository;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.event.repository.EventRepository;
import ru.yandex.practicum.exception.ConflictException;
import ru.yandex.practicum.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    CompilationRepository compilationRepository;
    EventRepository eventRepository;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto dto) {
        log.info("Создание подборки: {}", dto.getTitle());

        Compilation compilation = CompilationMapper.INSTANCE.toEntity(dto);

        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            List<Event> events = eventRepository.findAllById(dto.getEvents());
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
            return CompilationMapper.INSTANCE.toDto(saved);
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
            events = eventRepository.findAllById(request.getEvents());
        }

        CompilationMapper.INSTANCE.updateEntity(request, compilation, events);

        try {
            Compilation updated = compilationRepository.save(compilation);
            log.info("Подборка обновлена, id: {}", updated.getId());
            return CompilationMapper.INSTANCE.toDto(updated);
        } catch (DataIntegrityViolationException e) {
            log.error("Название уже существует: {}", request.getTitle());
            throw new ConflictException("Подборка с таким названием уже существует");
        }
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Pageable pageable) {
        log.info("Запрос подборок: pinned={}, page={}", pinned, pageable.getPageNumber());

        if (pinned == null) {
            return CompilationMapper.INSTANCE.toDto(compilationRepository.findAll(pageable).getContent());
        } else {
            return CompilationMapper.INSTANCE.toDto(compilationRepository.findByPinned(pinned, pageable).getContent());
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

        return CompilationMapper.INSTANCE.toDto(compilation);
    }
}
