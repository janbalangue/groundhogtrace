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
package dev.groundhogtrace.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RedactionServiceTest {
    private final RedactionService redactionService = new RedactionService(new ObjectMapper());

    @Test
    void redactsSensitiveHeaders() {
        String json = redactionService.redactHeadersToJson(Map.of(
                "Authorization", "Bearer secret",
                "Content-Type", "application/json"
        ));

        assertThat(json).contains("[REDACTED]");
        assertThat(json).doesNotContain("Bearer secret");
        assertThat(json).contains("application/json");
    }

    @Test
    void redactsSensitiveJsonFields() {
        String redacted = redactionService.redactBody("{\"email\":\"a@example.test\",\"apiKey\":\"abc\",\"nested\":{\"token\":\"xyz\"}}");

        assertThat(redacted).contains("[REDACTED]");
        assertThat(redacted).doesNotContain("abc");
        assertThat(redacted).doesNotContain("xyz");
        assertThat(redacted).contains("a@example.test");
    }
}
