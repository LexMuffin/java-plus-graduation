package ru.yandex.practicum.request.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.request.model.RequestStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ParticipationRequestDto {

    Long id;

    LocalDateTime created;

    Long event;

    Long requester;

    RequestStatus status;

}
