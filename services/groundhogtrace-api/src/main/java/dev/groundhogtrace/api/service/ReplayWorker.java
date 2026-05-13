package dev.groundhogtrace.api.service;

import dev.groundhogtrace.api.model.ReplayJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReplayWorker {
    private final ReplayService replayService;
    private final boolean enabled;
    private final int batchSize;

    public ReplayWorker(ReplayService replayService,
                        @Value("${replay.worker.enabled:true}") boolean enabled,
                        @Value("${replay.worker.batch-size:5}") int batchSize) {
        this.replayService = replayService;
        this.enabled = enabled;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${replay.worker.fixed-delay-ms:2000}")
    public void processQueuedJobs() {
        if (!enabled) {
            return;
        }
        replayService.findQueuedJobs()
                .stream()
                .limit(batchSize)
                .map(ReplayJob::getId)
                .forEach(replayService::executeJob);
    }
}
