package ru.yandex.practicum.compilations.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.dto.compilation.NewCompilationDto;
import ru.yandex.practicum.dto.compilation.UpdateCompilationRequest;
import ru.yandex.practicum.compilations.model.Compilation;
import ru.yandex.practicum.event.mapper.EventMapper;
import ru.yandex.practicum.event.model.Event;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = {EventMapper.class})
public interface CompilationMapper {

    @Mapping(target = "pinned", defaultValue = "false")
    Compilation toEntity(NewCompilationDto dto);

    CompilationDto toDto(Compilation compilation);

    List<CompilationDto> toDto(List<Compilation> compilations);

    @Named("updateEntity")
    default void updateEntity(UpdateCompilationRequest request, Compilation compilation, List<Event> events) {
        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (events != null) {
            compilation.setEvents(events);
        }
    }

    default List<Event> mapEvents(List<Long> eventIds) {
        if (eventIds == null) return new ArrayList<>();
        List<Event> events = new ArrayList<>();
        for (Long id : eventIds) {
            events.add(Event.builder().id(id).build());
        }
        return events;
    }
}
