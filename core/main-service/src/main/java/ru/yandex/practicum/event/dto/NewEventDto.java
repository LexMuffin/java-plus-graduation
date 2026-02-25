package ru.yandex.practicum.event.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewEventDto {

    @NotBlank
    @Size(min = 20, max = 2000)
    String annotation;

    @NotNull
    @Positive
    Long category;

    @NotBlank
    @Size(min = 20, max = 7000)
    String description;

    @NotNull
    LocalDateTime eventDate;

    @NotNull
    LocationDto location;

    Boolean paid;

    @PositiveOrZero
    Integer participantLimit;

    Boolean requestModeration;

    @NotBlank
    @Size(min = 3, max = 120)
    String title;
}
