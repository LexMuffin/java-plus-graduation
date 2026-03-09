package ru.yandex.practicum.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;


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
