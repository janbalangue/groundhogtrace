# GroundhogTrace

GroundhogTrace is a Java/Spring Boot failure-replay platform for REST integrations.

It captures failed HTTP interactions, redacts sensitive data, replays failures safely against sandbox/local targets, diffs original vs replayed behavior, and generates regression-test starter code from real failure scenarios.

## Technologies

- Java 21 and Spring Boot 3 microservices
- REST API capture and replay
- WebClient-based outbound HTTP execution
- PostgreSQL persistence with an H2 local fallback
- Redaction of sensitive headers and JSON fields
- Failure classification
- Async replay via a DB-backed worker queue
- Response/status diffing
- Generated JUnit/MockMvc regression-test snippets
- Docker Compose environment with a fake downstream CRM service

## Architecture

```text
Client / test harness
        |
        v
GroundhogTrace Capture API
        |
        +--> Redaction + validation
        |
        +--> PostgreSQL/H2 captures table
        |
        +--> Replay job queue
                  |
                  v
            Replay worker
                  |
                  v
           Fake downstream CRM API
                  |
                  v
             Diff + generated test artifact
```

This MVP intentionally uses a DB-backed replay queue instead of Kafka so it is easy to run. Kafka can be added later behind the replay job boundary.

## Run locally with Docker Compose

```bash
docker compose up --build
```

Services:

- GroundhogTrace API: http://localhost:8080
- Fake CRM API: http://localhost:8081
- PostgreSQL: localhost:5432

## Demo flow

Capture a synthetic failure:

```bash
curl -s -X POST http://localhost:8080/api/captures \
  -H 'Content-Type: application/json' \
  --data @examples/failing-capture.json
```

Create a replay job. Replace `<captureId>` with the ID returned above:

```bash
curl -s -X POST http://localhost:8080/api/replays \
  -H 'Content-Type: application/json' \
  -d '{
    "captureId": "<captureId>",
    "targetUrlOverride": "http://fake-crm-api:8081/fixed/customers"
  }'
```

Poll the replay result:

```bash
curl -s http://localhost:8080/api/replays/<jobId>
```

Generate a regression-test starter:

```bash
curl -s http://localhost:8080/api/replays/<jobId>/generated-test
```

## Local development without Docker

The API defaults to an in-memory H2 database when not running with the `docker` profile.

```bash
mvn -pl services/groundhogtrace-api spring-boot:run
mvn -pl services/fake-crm-api spring-boot:run
```

## Notes

- Do not use real production/customer payloads in this project.
- The fake CRM service exists only to demonstrate repeatable failure replay.
- Redaction is intentionally conservative and can be extended with custom rules.
