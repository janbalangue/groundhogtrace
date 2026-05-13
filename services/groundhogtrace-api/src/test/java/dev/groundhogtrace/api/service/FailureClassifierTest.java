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

import dev.groundhogtrace.api.model.FailureType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FailureClassifierTest {
    private final FailureClassifier classifier = new FailureClassifier();

    @Test
    void classifiesAuthFailures() {
        assertThat(classifier.classify(401, "unauthorized")).isEqualTo(FailureType.AUTH_FAILURE);
        assertThat(classifier.classify(403, "forbidden")).isEqualTo(FailureType.AUTH_FAILURE);
    }

    @Test
    void classifiesBadJson() {
        assertThat(classifier.classify(400, "malformed JSON payload")).isEqualTo(FailureType.BAD_JSON);
    }

    @Test
    void classifiesServerErrors() {
        assertThat(classifier.classify(500, "boom")).isEqualTo(FailureType.SERVER_ERROR);
    }
}
