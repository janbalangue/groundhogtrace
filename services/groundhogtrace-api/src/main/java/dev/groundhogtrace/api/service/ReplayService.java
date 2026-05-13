package dev.groundhogtrace.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.groundhogtrace.api.dto.CreateReplayRequest;
import dev.groundhogtrace.api.model.Capture;
import dev.groundhogtrace.api.model.ReplayJob;
import dev.groundhogtrace.api.model.ReplayResult;
import dev.groundhogtrace.api.model.ReplayStatus;
import dev.groundhogtrace.api.repository.CaptureRepository;
import dev.groundhogtrace.api.repository.ReplayJobRepository;
import dev.groundhogtrace.api.repository.ReplayResultRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReplayService {
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final ReplayJobRepository replayJobRepository;
    private final ReplayResultRepository replayResultRepository;
    private final CaptureRepository captureRepository;
    private final DiffService diffService;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final int timeoutSeconds;

    public ReplayService(ReplayJobRepository replayJobRepository,
                         ReplayResultRepository replayResultRepository,
                         CaptureRepository captureRepository,
                         DiffService diffService,
                         ObjectMapper objectMapper,
                         WebClient webClient,
                         @Value("${replay.worker.timeout-seconds:10}") int timeoutSeconds) {
        this.replayJobRepository = replayJobRepository;
        this.replayResultRepository = replayResultRepository;
        this.captureRepository = captureRepository;
        this.diffService = diffService;
        this.objectMapper = objectMapper;
        this.webClient = webClient;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Transactional
    public ReplayJob create(CreateReplayRequest request) {
        captureRepository.findById(request.captureId())
                .orElseThrow(() -> new IllegalArgumentException("Capture not found: " + request.captureId()));

        ReplayJob job = new ReplayJob();
        job.setCaptureId(request.captureId());
        job.setTargetUrlOverride(request.targetUrlOverride());
        return replayJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public ReplayJob getJob(UUID id) {
        return replayJobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Replay job not found: " + id));
    }

    @Transactional(readOnly = true)
    public ReplayResult getLatestResult(UUID jobId) {
        return replayResultRepository.findFirstByReplayJobIdOrderByCompletedAtDesc(jobId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ReplayJob> findQueuedJobs() {
        return replayJobRepository.findTop5ByStatusOrderByCreatedAtAsc(ReplayStatus.QUEUED);
    }

    @Transactional
    public void executeJob(UUID jobId) {
        ReplayJob job = replayJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Replay job not found: " + jobId));

        if (job.getStatus() != ReplayStatus.QUEUED) {
            return;
        }

        Capture capture = captureRepository.findById(job.getCaptureId())
                .orElseThrow(() -> new IllegalArgumentException("Capture not found: " + job.getCaptureId()));

        job.setStatus(ReplayStatus.RUNNING);
        job.setAttempts(job.getAttempts() + 1);
        job.setUpdatedAt(Instant.now());
        replayJobRepository.save(job);

        ReplayResult result = executeHttpReplay(job, capture);
        ReplayResult savedResult = replayResultRepository.save(result);
        diffService.createDiff(capture, savedResult);

        if (result.getErrorMessage() == null) {
            job.setStatus(ReplayStatus.SUCCEEDED);
            job.setLastError(null);
        } else if (job.getAttempts() >= job.getMaxAttempts()) {
            job.setStatus(ReplayStatus.FAILED);
            job.setLastError(result.getErrorMessage());
        } else {
            job.setStatus(ReplayStatus.QUEUED);
            job.setLastError(result.getErrorMessage());
        }
        job.setUpdatedAt(Instant.now());
        replayJobRepository.save(job);
    }

    private ReplayResult executeHttpReplay(ReplayJob job, Capture capture) {
        Instant startedAt = Instant.now();
        long startNanos = System.nanoTime();
        ReplayResult result = new ReplayResult();
        result.setReplayJobId(job.getId());
        result.setCaptureId(capture.getId());
        result.setStartedAt(startedAt);

        try {
            String url = job.getTargetUrlOverride() == null || job.getTargetUrlOverride().isBlank()
                    ? capture.getTargetUrl()
                    : job.getTargetUrlOverride();

            ResponseEntity<String> response = webClient
                    .method(HttpMethod.valueOf(capture.getMethod()))
                    .uri(url)
                    .headers(headers -> applyHeaders(headers, capture.getRequestHeadersJson()))
                    .body(BodyInserters.fromValue(capture.getRequestBody() == null ? "" : capture.getRequestBody()))
                    .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response != null) {
                result.setStatusCode(response.getStatusCode().value());
                result.setResponseBody(response.getBody());
            } else {
                result.setErrorMessage("No response returned by WebClient");
            }
        } catch (Exception e) {
            result.setErrorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            result.setLatencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis());
            result.setCompletedAt(Instant.now());
        }

        return result;
    }

    private void applyHeaders(HttpHeaders headers, String requestHeadersJson) {
        if (requestHeadersJson == null || requestHeadersJson.isBlank()) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            return;
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(requestHeadersJson, STRING_MAP);
            parsed.forEach((name, value) -> {
                if (name != null && value != null && !name.equalsIgnoreCase(HttpHeaders.HOST)) {
                    headers.set(name, value);
                }
            });
            if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
        } catch (Exception ignored) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
    }
}
