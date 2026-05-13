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

import dev.groundhogtrace.api.dto.CreateReplayRequest;
import dev.groundhogtrace.api.dto.GeneratedTestResponse;
import dev.groundhogtrace.api.dto.ReplayJobResponse;
import dev.groundhogtrace.api.model.DiffResult;
import dev.groundhogtrace.api.model.ReplayJob;
import dev.groundhogtrace.api.model.ReplayResult;
import dev.groundhogtrace.api.repository.DiffResultRepository;
import dev.groundhogtrace.api.service.ReplayService;
import dev.groundhogtrace.api.service.TestArtifactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/replays")
public class ReplayController {
    private final ReplayService replayService;
    private final DiffResultRepository diffResultRepository;
    private final TestArtifactService testArtifactService;

    public ReplayController(ReplayService replayService,
                            DiffResultRepository diffResultRepository,
                            TestArtifactService testArtifactService) {
        this.replayService = replayService;
        this.diffResultRepository = diffResultRepository;
        this.testArtifactService = testArtifactService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ReplayJobResponse create(@Valid @RequestBody CreateReplayRequest request) {
        ReplayJob job = replayService.create(request);
        return ReplayJobResponse.from(job, null, null);
    }

    @GetMapping("/{id}")
    public ReplayJobResponse get(@PathVariable UUID id) {
        ReplayJob job = replayService.getJob(id);
        ReplayResult result = replayService.getLatestResult(id);
        DiffResult diff = result == null ? null : diffResultRepository.findFirstByReplayResultIdOrderByCreatedAtDesc(result.getId()).orElse(null);
        return ReplayJobResponse.from(job, result, diff);
    }

    @GetMapping("/{id}/generated-test")
    public GeneratedTestResponse generatedTest(@PathVariable UUID id) {
        return new GeneratedTestResponse(id, "GeneratedGroundhogTraceRegressionTest.java", testArtifactService.generateMockMvcTest(id));
    }
}
