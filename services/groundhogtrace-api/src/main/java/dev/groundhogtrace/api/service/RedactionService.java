package dev.groundhogtrace.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RedactionService {
    public static final String REDACTED = "[REDACTED]";

    private static final Set<String> SENSITIVE_HEADER_NAMES = Set.of(
            "authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "api-key",
            "proxy-authorization"
    );

    private static final Set<String> SENSITIVE_FIELD_TOKENS = Set.of(
            "password",
            "passwd",
            "token",
            "secret",
            "apikey",
            "api_key",
            "ssn",
            "cookie",
            "authorization"
    );

    private final ObjectMapper objectMapper;

    public RedactionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String redactHeadersToJson(Map<String, String> headers) {
        Map<String, String> safeHeaders = new LinkedHashMap<>();
        if (headers != null) {
            headers.forEach((name, value) -> safeHeaders.put(name, isSensitiveHeader(name) ? REDACTED : value));
        }
        try {
            return objectMapper.writeValueAsString(safeHeaders);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize redacted headers", e);
        }
    }

    public String redactBody(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode redacted = redactJsonNode(root);
            return objectMapper.writeValueAsString(redacted);
        } catch (JsonProcessingException ignored) {
            return body;
        }
    }

    private boolean isSensitiveHeader(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        return SENSITIVE_HEADER_NAMES.contains(normalized)
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("key");
    }

    private JsonNode redactJsonNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode object = ((ObjectNode) node).deepCopy();
            object.fieldNames().forEachRemaining(fieldName -> {
                JsonNode child = object.get(fieldName);
                if (isSensitiveField(fieldName)) {
                    object.put(fieldName, REDACTED);
                } else {
                    object.set(fieldName, redactJsonNode(child));
                }
            });
            return object;
        }
        if (node.isArray()) {
            ArrayNode original = (ArrayNode) node;
            ArrayNode redacted = objectMapper.createArrayNode();
            original.forEach(child -> redacted.add(redactJsonNode(child)));
            return redacted;
        }
        return node;
    }

    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT).replace("-", "_");
        return SENSITIVE_FIELD_TOKENS.stream().anyMatch(normalized::contains);
    }
}
