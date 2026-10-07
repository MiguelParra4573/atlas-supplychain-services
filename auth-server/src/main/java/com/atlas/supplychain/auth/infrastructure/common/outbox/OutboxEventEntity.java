package com.atlas.supplychain.auth.infrastructure.common.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "outbox_event", schema = "auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventEntity {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "aggregate_type", length = 100, nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 100, nullable = false)
    private String aggregateId;

    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "correlation_id", length = 36, nullable = false)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed", nullable = false)
    private boolean processed;

    @Column(name = "processed_at")
    private Instant processedAt;
}
