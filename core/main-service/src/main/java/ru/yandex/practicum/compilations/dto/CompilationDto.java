package ru.yandex.practicum.compilations.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.event.dto.EventShortDto;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationDto {

    Long id;

    List<EventShortDto> events;

    Boolean pinned;

    String title;
}
