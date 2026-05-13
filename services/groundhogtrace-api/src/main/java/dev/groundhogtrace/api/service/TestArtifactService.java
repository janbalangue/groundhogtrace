package dev.groundhogtrace.api.service;

import dev.groundhogtrace.api.model.Capture;
import dev.groundhogtrace.api.model.ReplayJob;
import dev.groundhogtrace.api.model.ReplayResult;
import dev.groundhogtrace.api.repository.CaptureRepository;
import dev.groundhogtrace.api.repository.ReplayJobRepository;
import dev.groundhogtrace.api.repository.ReplayResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TestArtifactService {
    private final ReplayJobRepository replayJobRepository;
    private final ReplayResultRepository replayResultRepository;
    private final CaptureRepository captureRepository;

    public TestArtifactService(ReplayJobRepository replayJobRepository,
                               ReplayResultRepository replayResultRepository,
                               CaptureRepository captureRepository) {
        this.replayJobRepository = replayJobRepository;
        this.replayResultRepository = replayResultRepository;
        this.captureRepository = captureRepository;
    }

    @Transactional(readOnly = true)
    public String generateMockMvcTest(UUID replayJobId) {
        ReplayJob job = replayJobRepository.findById(replayJobId)
                .orElseThrow(() -> new IllegalArgumentException("Replay job not found: " + replayJobId));
        Capture capture = captureRepository.findById(job.getCaptureId())
                .orElseThrow(() -> new IllegalArgumentException("Capture not found: " + job.getCaptureId()));
        ReplayResult result = replayResultRepository.findFirstByReplayJobIdOrderByCompletedAtDesc(replayJobId)
                .orElseThrow(() -> new IllegalStateException("Replay job has no result yet: " + replayJobId));

        int expectedStatus = result.getStatusCode() == null ? 500 : result.getStatusCode();
        String method = capture.getMethod().toLowerCase();
        String pathComment = job.getTargetUrlOverride() == null ? capture.getTargetUrl() : job.getTargetUrlOverride();
        String requestBody = escapeJavaTextBlock(capture.getRequestBody() == null ? "" : capture.getRequestBody());

        return """
                package example.generated;

                import org.junit.jupiter.api.Test;
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
                import org.springframework.boot.test.context.SpringBootTest;
                import org.springframework.http.MediaType;
                import org.springframework.test.web.servlet.MockMvc;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.%s;
                import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

                @SpringBootTest
                @AutoConfigureMockMvc
                class GeneratedGroundhogTraceRegressionTest {
                    @Autowired
                    MockMvc mockMvc;

                    @Test
                    void shouldPreserveReplayedBehavior() throws Exception {
                        // Generated from GroundhogTrace replay job: %s
                        // Original target: %s
                        String requestJson = \"\"\"
                %s
                                \"\"\";

                        mockMvc.perform(%s("/replace-with-local-controller-path")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestJson))
                                .andExpect(status().is(%d));
                    }
                }
                """.formatted(method, replayJobId, pathComment, indent(requestBody, 24), method, expectedStatus);
    }

    private String escapeJavaTextBlock(String value) {
        return value.replace("\\", "\\\\").replace("\"\"\"", "\\\"\\\"\\\"");
    }

    private String indent(String value, int spaces) {
        String indentation = " ".repeat(spaces);
        return indentation + value.replace("\n", "\n" + indentation);
    }
}
