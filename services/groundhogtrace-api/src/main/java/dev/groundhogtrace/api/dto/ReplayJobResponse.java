/*
 * Copyright 2026 Jan Balangue
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.groundhogtrace.api.dto;

import dev.groundhogtrace.api.model.DiffResult;
import dev.groundhogtrace.api.model.ReplayJob;
import dev.groundhogtrace.api.model.ReplayResult;
import dev.groundhogtrace.api.model.ReplayStatus;

import java.time.Instant;
import java.util.UUID;

public record ReplayJobResponse(
        UUID id,
        UUID captureId,
        String targetUrlOverride,
        ReplayStatus status,
        int attempts,
        int maxAttempts,
        String lastError,
        Instant createdAt,
        Instant updatedAt,
        ReplayResultSummary result,
        DiffResultSummary diff
) {
    public static ReplayJobResponse from(ReplayJob job, ReplayResult result, DiffResult diff) {
        return new ReplayJobResponse(
                job.getId(),
                job.getCaptureId(),
                job.getTargetUrlOverride(),
                job.getStatus(),
                job.getAttempts(),
                job.getMaxAttempts(),
                job.getLastError(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                result == null ? null : ReplayResultSummary.from(result),
                diff == null ? null : DiffResultSummary.from(diff)
        );
    }

    public record ReplayResultSummary(
            UUID id,
            Integer statusCode,
            String responseBody,
            long latencyMs,
            String errorMessage,
            Instant startedAt,
            Instant completedAt
    ) {
        static ReplayResultSummary from(ReplayResult result) {
            return new ReplayResultSummary(
                    result.getId(),
                    result.getStatusCode(),
                    result.getResponseBody(),
                    result.getLatencyMs(),
                    result.getErrorMessage(),
                    result.getStartedAt(),
                    result.getCompletedAt()
            );
        }
    }

    public record DiffResultSummary(
            UUID id,
            boolean statusChanged,
            boolean bodyChanged,
            Integer originalStatus,
            Integer replayStatus,
            String summary,
            Instant createdAt
    ) {
        static DiffResultSummary from(DiffResult diff) {
            return new DiffResultSummary(
                    diff.getId(),
                    diff.isStatusChanged(),
                    diff.isBodyChanged(),
                    diff.getOriginalStatus(),
                    diff.getReplayStatus(),
                    diff.getSummary(),
                    diff.getCreatedAt()
            );
        }
    }
}
