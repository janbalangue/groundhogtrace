package dev.groundhogtrace.api.service;

import dev.groundhogtrace.api.dto.CreateCaptureRequest;
import dev.groundhogtrace.api.model.Capture;
import dev.groundhogtrace.api.model.FailureType;
import dev.groundhogtrace.api.repository.CaptureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CaptureService {
    private final CaptureRepository captureRepository;
    private final RedactionService redactionService;
    private final FailureClassifier failureClassifier;

    public CaptureService(CaptureRepository captureRepository,
                          RedactionService redactionService,
                          FailureClassifier failureClassifier) {
        this.captureRepository = captureRepository;
        this.redactionService = redactionService;
        this.failureClassifier = failureClassifier;
    }

    @Transactional
    public Capture create(CreateCaptureRequest request) {
        Capture capture = new Capture();
        capture.setServiceName(request.serviceName());
        capture.setTargetService(request.targetService());
        capture.setMethod(request.method().toUpperCase(Locale.ROOT));
        capture.setTargetUrl(request.targetUrl());
        capture.setRequestHeadersJson(redactionService.redactHeadersToJson(request.requestHeaders()));
        capture.setRequestBody(redactionService.redactBody(request.requestBody()));
        capture.setResponseStatus(request.responseStatus());
        capture.setResponseBody(redactionService.redactBody(request.responseBody()));
        capture.setTraceId(request.traceId());
        capture.setFailureType(failureClassifier.classify(request.responseStatus(), request.responseBody()));
        capture.setRedactionApplied(true);
        return captureRepository.save(capture);
    }

    @Transactional(readOnly = true)
    public Capture get(UUID id) {
        return captureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Capture not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Capture> search(String serviceName, FailureType failureType) {
        if (serviceName != null && !serviceName.isBlank()) {
            return captureRepository.findTop50ByServiceNameOrderByCreatedAtDesc(serviceName);
        }
        if (failureType != null) {
            return captureRepository.findTop50ByFailureTypeOrderByCreatedAtDesc(failureType);
        }
        return captureRepository.findTop50ByOrderByCreatedAtDesc();
    }
}
