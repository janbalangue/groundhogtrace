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

import dev.groundhogtrace.api.model.Capture;
import dev.groundhogtrace.api.model.FailureType;

import java.time.Instant;
import java.util.UUID;

public record CaptureResponse(
        UUID id,
        String serviceName,
        String targetService,
        String method,
        String targetUrl,
        String requestHeadersJson,
        String requestBody,
        Integer responseStatus,
        String responseBody,
        String traceId,
        FailureType failureType,
        boolean redactionApplied,
        Instant createdAt
) {
    public static CaptureResponse from(Capture capture) {
        return new CaptureResponse(
                capture.getId(),
                capture.getServiceName(),
                capture.getTargetService(),
                capture.getMethod(),
                capture.getTargetUrl(),
                capture.getRequestHeadersJson(),
                capture.getRequestBody(),
                capture.getResponseStatus(),
                capture.getResponseBody(),
                capture.getTraceId(),
                capture.getFailureType(),
                capture.isRedactionApplied(),
                capture.getCreatedAt()
        );
    }
}
