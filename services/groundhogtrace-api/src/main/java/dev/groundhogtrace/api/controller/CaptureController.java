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
package dev.groundhogtrace.api.controller;

import dev.groundhogtrace.api.dto.CaptureResponse;
import dev.groundhogtrace.api.dto.CreateCaptureRequest;
import dev.groundhogtrace.api.model.FailureType;
import dev.groundhogtrace.api.service.CaptureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/captures")
public class CaptureController {
    private final CaptureService captureService;

    public CaptureController(CaptureService captureService) {
        this.captureService = captureService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaptureResponse create(@Valid @RequestBody CreateCaptureRequest request) {
        return CaptureResponse.from(captureService.create(request));
    }

    @GetMapping("/{id}")
    public CaptureResponse get(@PathVariable UUID id) {
        return CaptureResponse.from(captureService.get(id));
    }

    @GetMapping
    public List<CaptureResponse> search(@RequestParam(required = false) String serviceName,
                                        @RequestParam(required = false) FailureType failureType) {
        return captureService.search(serviceName, failureType).stream()
                .map(CaptureResponse::from)
                .toList();
    }
}
