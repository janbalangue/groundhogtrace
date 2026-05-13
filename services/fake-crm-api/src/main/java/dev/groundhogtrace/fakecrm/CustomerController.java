package dev.groundhogtrace.fakecrm;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping
public class CustomerController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/buggy/customers")
    public ResponseEntity<Map<String, Object>> createCustomerBuggy(@RequestBody CustomerRequest request) {
        if (request.email() == null || !request.email().contains("@")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "NullPointerException while normalizing customer email"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("customerId", "crm_" + UUID.randomUUID(), "status", "created"));
    }

    @PostMapping("/fixed/customers")
    public ResponseEntity<Map<String, Object>> createCustomerFixed(@RequestBody CustomerRequest request) {
        if (request.email() == null || !request.email().contains("@")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "email must be a valid email address", "field", "email"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("customerId", "crm_" + UUID.randomUUID(), "status", "created"));
    }

    public record CustomerRequest(String customerId, String email) {
    }
}
