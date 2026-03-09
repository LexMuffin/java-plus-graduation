package ru.yandex.practicum.request.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.dto.request.RequestStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "participation_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ParticipationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    LocalDateTime created;

    @JoinColumn(name = "event_id", nullable = false)
    Long event;

    @JoinColumn(name = "requester_id", nullable = false)
    Long requester;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    RequestStatus status;
}
