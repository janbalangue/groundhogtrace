# Demo

## 5-minute walkthrough

1. Start the stack.

```bash
docker compose up --build
```

2. Submit a captured production-like failure.

```bash
curl -s -X POST http://localhost:8080/api/captures \
  -H 'Content-Type: application/json' \
  --data @examples/failing-capture.json
```

3. Replay the captured failure against the fixed fake CRM endpoint.

```bash
curl -s -X POST http://localhost:8080/api/replays \
  -H 'Content-Type: application/json' \
  -d '{"captureId":"<captureId>","targetUrlOverride":"http://fake-crm-api:8081/fixed/customers"}'
```

4. Poll the replay job.

```bash
curl -s http://localhost:8080/api/replays/<jobId>
```

5. Generate a test starter.

```bash
curl -s http://localhost:8080/api/replays/<jobId>/generated-test
```

## Under the hood

- The capture pipeline redacts secrets before persistence.
- Replay jobs are asynchronous and stateful.
- WebClient is used to execute captured HTTP requests.
- Diffing turns repeated failures into a fix-verification workflow.
- Generated tests convert incidents into regression coverage.
- The architecture intentionally leaves clean seams for Kafka, OpenTelemetry, and multi-tenant controls.
