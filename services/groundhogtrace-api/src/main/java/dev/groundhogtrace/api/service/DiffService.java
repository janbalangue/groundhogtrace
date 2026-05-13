package dev.groundhogtrace.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.groundhogtrace.api.model.Capture;
import dev.groundhogtrace.api.model.DiffResult;
import dev.groundhogtrace.api.model.ReplayResult;
import dev.groundhogtrace.api.repository.DiffResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class DiffService {
    private final DiffResultRepository diffResultRepository;
    private final ObjectMapper objectMapper;

    public DiffService(DiffResultRepository diffResultRepository, ObjectMapper objectMapper) {
        this.diffResultRepository = diffResultRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DiffResult createDiff(Capture capture, ReplayResult replayResult) {
        boolean statusChanged = !Objects.equals(capture.getResponseStatus(), replayResult.getStatusCode());
        boolean bodyChanged = !Objects.equals(normalize(capture.getResponseBody()), normalize(replayResult.getResponseBody()));

        DiffResult diff = new DiffResult();
        diff.setCaptureId(capture.getId());
        diff.setReplayResultId(replayResult.getId());
        diff.setOriginalStatus(capture.getResponseStatus());
        diff.setReplayStatus(replayResult.getStatusCode());
        diff.setStatusChanged(statusChanged);
        diff.setBodyChanged(bodyChanged);
        diff.setSummary(buildSummary(capture, replayResult, statusChanged, bodyChanged));
        return diffResultRepository.save(diff);
    }

    private String normalize(String body) {
        if (body == null) {
            return null;
        }
        try {
            JsonNode json = objectMapper.readTree(body);
            return objectMapper.writeValueAsString(json);
        } catch (Exception ignored) {
            return body.trim();
        }
    }

    private String buildSummary(Capture capture, ReplayResult result, boolean statusChanged, boolean bodyChanged) {
        if (result.getErrorMessage() != null) {
            return "Replay failed before receiving a response: " + result.getErrorMessage();
        }
        if (!statusChanged && !bodyChanged) {
            return "Failure still reproduces. Status and response body match the original capture.";
        }
        if (statusChanged && isSuccess(result.getStatusCode())) {
            return "Failure appears fixed. Status changed from " + capture.getResponseStatus() + " to " + result.getStatusCode() + ".";
        }
        if (statusChanged) {
            return "Behavior changed. Status changed from " + capture.getResponseStatus() + " to " + result.getStatusCode() + ".";
        }
        return "Response body changed while status remained " + result.getStatusCode() + ".";
    }

    private boolean isSuccess(Integer statusCode) {
        return statusCode != null && statusCode >= 200 && statusCode <= 299;
    }
}
