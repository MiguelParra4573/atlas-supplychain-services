package com.atlas.supplychain.auth.infrastructure.common.outbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, String> {

    List<OutboxEventEntity> findTop100ByProcessedFalseOrderByCreatedAtAsc();
}
