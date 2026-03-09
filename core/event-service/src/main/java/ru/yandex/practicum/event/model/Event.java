package ru.yandex.practicum.event.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.dto.event.EventState;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(length = 2000, nullable = false)
    String annotation;

    @JoinColumn(name = "category_id", nullable = false)
    Long category;

    @Column(name = "confirmed_requests")
    @Builder.Default
    Long confirmedRequests = 0L;

    @Column(name = "created_on", nullable = false)
    @Builder.Default
    LocalDateTime createdOn = LocalDateTime.now();

    @Column(length = 7000, nullable = false)
    String description;

    @Column(name = "event_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime eventDate;

    @JoinColumn(name = "initiator_id", nullable = false)
    Long initiator;

    @Embedded
    Location location;

    @Column(nullable = false)
    @Builder.Default
    Boolean paid = false;

    @Column(name = "participant_limit")
    @Builder.Default
    Integer participantLimit = 0;

    @Column(name = "published_on")
    LocalDateTime publishedOn;

    @Column(name = "request_moderation")
    @Builder.Default
    Boolean requestModeration = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    @Builder.Default
    EventState state = EventState.PENDING;

    @Column(length = 120, nullable = false)
    String title;

    @Column
    @Builder.Default
    Long views = 0L;
}
