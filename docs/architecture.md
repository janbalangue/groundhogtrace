# Architecture

GroundhogTrace is split into two runnable services for the MVP:

1. `groundhogtrace-api` - captures failed HTTP interactions, persists them, creates replay jobs, runs the replay worker, stores diffs, and generates test snippets.
2. `fake-crm-api` - a deterministic downstream API used to demonstrate a failure and a later fixed behavior.

## Why a DB-backed queue?

The MVP uses a `replay_jobs` table as a queue. This keeps local setup simple while still exposing the same concepts as a message-driven design:

- queued work
- retry attempts
- worker lifecycle
- idempotency boundaries
- failure states
- dead-letter candidates

A Kafka-backed version can replace `ReplayJobRepository` polling later without changing the public API.

## Core flow

```text
POST /api/captures
  -> CaptureService
  -> RedactionService
  -> FailureClassifier
  -> CaptureRepository

POST /api/replays
  -> ReplayService creates QUEUED replay job

Scheduled ReplayWorker
  -> loads queued job
  -> executes WebClient request
  -> stores ReplayResult
  -> stores DiffResult
```

## MVP boundaries

Included:

- Redaction
- Capture persistence
- Replay job lifecycle
- Status/body diffing
- Generated test starter
- Fake downstream service

Deferred:

- Kafka topics
- OpenTelemetry export
- RBAC
- Multi-tenant rate limiting
- Full JSON schema-drift detection
- Web UI
