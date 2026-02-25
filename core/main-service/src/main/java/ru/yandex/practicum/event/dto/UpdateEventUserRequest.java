package ru.yandex.practicum.event.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.event.model.EventStateAction;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateEventUserRequest {

    String annotation;

    Long category;

    String description;

    LocalDateTime eventDate;

    LocationDto location;

    Boolean paid;

    Integer participantLimit;

    Boolean requestModeration;

    EventStateAction stateAction;

    String title;
}
