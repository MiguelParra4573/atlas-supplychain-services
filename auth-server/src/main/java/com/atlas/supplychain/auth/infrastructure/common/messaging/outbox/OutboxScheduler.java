package com.atlas.supplychain.auth.infrastructure.common.messaging.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRelayService outboxRelayService;

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:5000}")
    public void relayPendingEvents() {
        outboxRelayService.relay();
    }
}
