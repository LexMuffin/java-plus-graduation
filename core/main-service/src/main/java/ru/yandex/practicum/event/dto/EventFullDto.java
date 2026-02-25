package ru.yandex.practicum.event.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.category.dto.CategoryDto;
import ru.yandex.practicum.event.model.EventState;
import ru.yandex.practicum.user.dto.UserShortDto;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventFullDto {

    Long id;

    String annotation;

    CategoryDto category;

    Long confirmedRequests;

    LocalDateTime createdOn;

    String description;

    LocalDateTime eventDate;

    UserShortDto initiator;

    LocationDto location;

    Boolean paid;

    Integer participantLimit;

    LocalDateTime publishedOn;

    Boolean requestModeration;

    EventState state;

    String title;

    Long views;
}
