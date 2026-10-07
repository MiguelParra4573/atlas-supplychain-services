package com.atlas.supplychain.auth.infrastructure.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, String> {

    List<OutboxEventEntity> findTop100ByProcessedFalseOrderByCreatedAtAsc();
}
