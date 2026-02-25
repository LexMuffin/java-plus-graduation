package ru.yandex.practicum.event.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.category.dto.CategoryDto;
import ru.yandex.practicum.user.dto.UserShortDto;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventShortDto {

    Long id;

    String annotation;

    CategoryDto category;

    Long confirmedRequests;

    LocalDateTime eventDate;

    UserShortDto initiator;

    Boolean paid;

    String title;

    Long views;
}
