package ru.yandex.practicum.compilations.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import ru.yandex.practicum.compilations.dto.CompilationDto;
import ru.yandex.practicum.compilations.dto.NewCompilationDto;
import ru.yandex.practicum.compilations.dto.UpdateCompilationRequest;
import ru.yandex.practicum.compilations.model.Compilation;
import ru.yandex.practicum.event.mapper.EventMapper;
import ru.yandex.practicum.event.model.Event;

import java.util.List;

@Mapper(uses = {EventMapper.class})
public interface CompilationMapper {

    CompilationMapper INSTANCE = Mappers.getMapper(CompilationMapper.class);

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
}
