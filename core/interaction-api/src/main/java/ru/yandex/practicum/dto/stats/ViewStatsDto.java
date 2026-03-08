package ru.yandex.practicum.dto.stats;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ViewStatsDto {
    @NotBlank
    String app;

    @NotBlank
    String uri;

    Long hits;
}
