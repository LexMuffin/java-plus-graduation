package ru.yandex.practicum.request.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.request.model.RequestStatus;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventRequestStatusUpdateRequest {

    List<Long> requestIds;

    RequestStatus status;
}
