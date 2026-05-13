package dev.groundhogtrace.api.service;

import dev.groundhogtrace.api.model.FailureType;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class FailureClassifier {
    public FailureType classify(Integer status, String responseBody) {
        if (status == null) {
            return FailureType.NETWORK_FAILURE;
        }
        if (status == 401 || status == 403) {
            return FailureType.AUTH_FAILURE;
        }
        if (status == 429) {
            return FailureType.RATE_LIMITED;
        }
        if (status == 408 || status == 504) {
            return FailureType.DOWNSTREAM_TIMEOUT;
        }
        if (status == 400 && bodyLooksLikeJsonParsingFailure(responseBody)) {
            return FailureType.BAD_JSON;
        }
        if (status >= 500 && status <= 599) {
            return FailureType.SERVER_ERROR;
        }
        if (status >= 400 && status <= 499) {
            return FailureType.CLIENT_ERROR;
        }
        return FailureType.UNKNOWN;
    }

    private boolean bodyLooksLikeJsonParsingFailure(String responseBody) {
        if (responseBody == null) {
            return false;
        }
        String normalized = responseBody.toLowerCase(Locale.ROOT);
        return normalized.contains("json") || normalized.contains("parse") || normalized.contains("malformed");
    }
}
