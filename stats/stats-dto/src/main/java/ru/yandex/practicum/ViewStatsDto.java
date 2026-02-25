package ru.yandex.practicum;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ViewStatsDto {
    String app;
    String uri;
    Long hits;
}
