# razorpay-provider-service

A production-grade microservice responsible for all direct communication with the Razorpay Payment Gateway. Part of the **Razorpay Payment Integration System** — a four-service microservices architecture designed for independent deployability, fault isolation, and extensibility.

This service is the **only** component in the system that knows Razorpay exists. All other services (processing, validation, gateway) are provider-agnostic.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Responsibilities](#responsibilities)
- [Payment Lifecycle](#payment-lifecycle)
- [Project Structure](#project-structure)
- [API Reference](#api-reference)
- [Error Handling](#error-handling)
- [Database Schema](#database-schema)
- [Resilience](#resilience)
- [Security](#security)
- [Configuration](#configuration)
- [Running Locally](#running-locally)
- [Tech Stack](#tech-stack)

---

## Architecture Overview

```
api-gateway
    │
    ▼
payment-validation-service
    │
    ▼
payment-processing-service
    │
    ▼
razorpay-provider-service  ◄──── this service
    │                  ▲
    ▼                  │
Razorpay API      Razorpay Webhooks
```

This service sits at the boundary between your internal payment system and Razorpay's external API. It exposes internal HTTP endpoints consumed by the processing service, and a webhook endpoint consumed directly by Razorpay's servers.

Adding a new payment provider (e.g. PayPal, Stripe) requires only deploying a new provider service — zero changes to validation, processing, or gateway services.

---

## Responsibilities

- Create payment orders with Razorpay via the Orders API
- Verify HMAC-SHA256 signatures from the Razorpay checkout popup (handler path)
- Call the Razorpay Capture API after signature verification (manual capture mode)
- Receive and verify Razorpay webhook events via HMAC-SHA256 signature validation
- Persist Razorpay-specific order and event data independently
- Detect and reject concurrent status transition conflicts at the database level
- Guarantee idempotent webhook processing via PostgreSQL `ON CONFLICT DO NOTHING`
- Publish payment events to Kafka for downstream consumption by the processing service

---

## Payment Lifecycle

This service implements **manual capture mode** (auto-capture disabled). This is intentional — funds are reserved on the customer's payment method at authorization, and only captured after your server has independently verified the payment's authenticity.

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PAYMENT LIFECYCLE                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. Processing service calls POST /v1/internal/orders               │
│     └─► OrderService creates order with Razorpay                    │
│     └─► Razorpay returns razorpay_order_id                          │
│     └─► Order persisted with status: CREATED                        │
│                                                                     │
│  2. Frontend opens Razorpay checkout popup using razorpay_order_id  │
│     └─► Customer completes payment (card / UPI / netbanking)        │
│     └─► Payment status on Razorpay: AUTHORIZED (not yet captured)   │
│     └─► Popup handler delivers:                                     │
│           razorpay_payment_id                                       │
│           razorpay_order_id                                         │
│           razorpay_signature                                        │
│                                                                     │
│  3. Frontend calls POST /v1/internal/payments/verify-and-capture    │
│     └─► HMAC-SHA256 signature verified using API key secret         │
│         Formula: HMAC(orderId + "|" + paymentId, key_secret)        │
│     └─► Razorpay Capture API called on successful verification      │
│     └─► Order status updated: CREATED → AUTHORIZED                  │
│     └─► Response returned immediately: status AUTHORIZED            │
│                                                                     │
│  4. Razorpay fires payment.captured webhook (async)                 │
│     └─► POST /v1/internal/payments/webhook                          │
│     └─► Webhook HMAC verified using separate webhook secret         │
│         Formula: HMAC(rawRequestBody, webhook_secret)               │
│     └─► Event inserted with idempotency guard                       │
│     └─► Order status updated: AUTHORIZED → CAPTURED                 │
│     └─► PaymentCapturedEvent published to Kafka                     │
│     └─► Processing service consumes event → marks payment SUCCESS   │
│                                                                     │
│  Status machine (this service, Razorpay-side status only):          │
│  CREATED → AUTHORIZED → CAPTURED                                    │
│                       → FAILED                                      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

> **Why two separate HMAC verifications?**
> The handler-path signature uses your **API key secret** and a payload of `orderId|paymentId`. The webhook signature uses a **separate webhook secret** and the raw request body. Different secrets, different formulas, different attack surfaces — intentionally independent.

---

## Project Structure

```
razorpay-provider-service/
│
├── client/                          # Feign client — Razorpay API communication
│   ├── RazorpayClient.java          # Feign interface: createOrder, capturePayment
│   ├── config/
│   │   ├── FeignHttpClientConfig.java
│   │   └── RazorpayFeignConfig.java  # Auth interceptor, error decoder registration
│   ├── decoder/
│   │   └── RazorpayErrorDecoder.java # Translates Razorpay HTTP errors → domain exceptions
│   └── model/                        # Razorpay API request/response models
│       ├── RazorpayOrderRequest.java
│       ├── RazorpayOrderResponse.java
│       ├── RazorpayCaptureRequest.java
│       └── RazorpayCaptureResponse.java
│
├── constant/
│   ├── RazorpayConstants.java        # Paise conversion, receipt prefix, defaults
│   └── WebhookConstants.java         # Webhook event type constants
│
├── controller/
│   ├── OrderController.java          # POST /v1/internal/orders
│   ├── PaymentController.java        # POST /v1/internal/payments/verify-and-capture
│   └── WebhookController.java        # POST /v1/internal/payments/webhook
│
├── dto/                              # Internal API contracts
│   ├── OrderRequest.java
│   ├── OrderResponse.java
│   ├── PaymentCaptureRequest.java
│   └── PaymentCaptureResponse.java
│
├── exception/
│   ├── ErrorCode.java                # Enum: error code + HttpStatus + retryable flag
│   ├── ErrorResponse.java            # Structured error response contract
│   ├── GlobalExceptionHandler.java   # @RestControllerAdvice — single error handling entry point
│   ├── RazorpayProviderException.java
│   └── resolver/
│       └── ErrorMessageResolver.java # Resolves messages from messages.properties via MessageSource
│
├── repository/
│   ├── RazorpayOrderRepository.java        # Spring JDBC — razorpay_orders table
│   ├── RazorpayPaymentEventRepository.java # Spring JDBC — razorpay_payment_events table
│   └── entity/
│       ├── RazorpayOrderEntity.java
│       └── RazorpayPaymentEventEntity.java
│
├── service/
│   ├── OrderService.java      # Order creation orchestration
│   ├── PaymentService.java    # Signature verification + capture (handler path)
│   ├── WebhookService.java    # Webhook verification + event processing (webhook path)
│   └── helper/
│       ├── HmacSignatureVerifier.java  # Shared HMAC-SHA256 utility
│       └── UniqueIdGenerator.java      # Internal order ID generation
│
└── resources/
    ├── application.properties
    ├── application-dev.properties
    ├── application-local.properties
    ├── application-prod.properties
    ├── application-qa.properties
    ├── messages.properties             # Externalised error messages (i18n-ready)
    └── db/migration/
        └── V1__razorpay_provider_schema.sql
```

---

## API Reference

All endpoints are internal — consumed by the processing service or Razorpay's servers directly. Not exposed through the API gateway to merchants.

### Create Order

```
POST /v1/internal/orders
```

Creates a payment order with Razorpay. Returns the `razorpay_order_id` required to initialise the frontend checkout popup.

**Request:**
```json
{
  "amount": 500,
  "currency": "INR"
}
```

**Response `200 OK`:**
```json
{
  "orderId": "order_OBFaKkjdsalK",
  "amount": 500,
  "currency": "INR",
  "receipt": "RCPT_a3f9b2c1d4",
  "rpStatus": "created"
}
```

---

### Verify and Capture

```
POST /v1/internal/payments/verify-and-capture
```

Verifies the HMAC-SHA256 signature received from the Razorpay checkout popup, then calls the Razorpay Capture API. This is Path A of the dual-confirmation flow.

**Request:**
```json
{
  "razorpayPaymentId": "pay_OBFaKkjdsalL",
  "razorpayOrderId":   "order_OBFaKkjdsalK",
  "razorpaySignature": "a1b2c3d4e5f6...",
  "amount": 500,
  "currency": "INR"
}
```

**Response `200 OK`:**
```json
{
  "razorpayPaymentId": "pay_OBFaKkjdsalL",
  "razorpayOrderId":   "order_OBFaKkjdsalK",
  "status": "AUTHORIZED",
  "amount": 500,
  "currency": "INR"
}
```

> **Why `AUTHORIZED` and not `SUCCESS`?**
> The `SUCCESS` state transition is owned exclusively by the webhook path (`payment.captured` event). This response confirms the capture API call succeeded — the webhook confirms the funds moved. This separation ensures your system's source of truth is always Razorpay's server-to-server confirmation, never a client-side callback alone.

---

### Webhook

```
POST /v1/internal/payments/webhook
Header: X-Razorpay-Signature: <hmac_signature>
```

Receives asynchronous payment events from Razorpay's servers. Verifies the request's HMAC-SHA256 signature before processing. Returns `200 OK` immediately — all processing happens before the response to meet Razorpay's acknowledgement timeout.

**Handled events:**

| Event | Action |
|---|---|
| `payment.captured` | Updates order status to `CAPTURED`, publishes event to Kafka |
| `payment.failed` | Updates order status to `FAILED`, publishes event to Kafka |
| Others | Logged and acknowledged — no state change |

> **Important:** The webhook endpoint takes `@RequestBody String rawPayload` — the body is never parsed before signature verification. Parsing and re-serialising JSON before verification would change the byte sequence and break the HMAC check.

---

## Error Handling

All errors are handled by a single `GlobalExceptionHandler` (`@RestControllerAdvice`). No try-catch blocks in controllers or service layer.

**Error response structure:**

```json
{
  "errorCode": 3003,
  "message": "Webhook signature verification failed. Possible spoofed request.",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "timestamp": "2025-10-14T08:32:11.452Z",
  "path": "/v1/internal/payments/webhook",
  "retryable": false,
  "details": {
    "razorpayOrderId": "order_OBFaKkjdsalK"
  }
}
```

**Design decisions:**
- `retryable` flag is declared on the `ErrorCode` enum — no ad-hoc logic in handlers
- `HttpStatus` mapping lives on the enum — controllers and services have zero HTTP knowledge
- Error messages are externalised to `messages.properties` — i18n-ready, no strings in Java code
- `details` map carries structured context (e.g. `razorpayOrderId`, `razorpayErrorCode`) without exposing internal stack traces

---

## Database Schema

PostgreSQL 18 — database: `razorpay_provider_db`

Managed by Flyway versioned migrations (`V1__razorpay_provider_schema.sql`).

### `razorpay_orders`

Tracks one record per payment order. Created on order creation, updated on capture and webhook receipt.

| Column | Type | Description |
|---|---|---|
| `id` | BIGSERIAL | Internal surrogate key |
| `internal_order_id` | VARCHAR(50) | Correlation ID linking to processing service |
| `razorpay_order_id` | VARCHAR(100) | Razorpay's order ID — unique |
| `razorpay_payment_id` | VARCHAR(100) | Razorpay's payment ID — populated after checkout |
| `amount_paise` | BIGINT | Amount in smallest currency unit (paise) — matches Razorpay's API |
| `currency` | VARCHAR(3) | ISO 4217 currency code |
| `receipt` | VARCHAR(50) | Your generated receipt ID |
| `razorpay_status` | VARCHAR(20) | `CREATED` / `AUTHORIZED` / `CAPTURED` / `FAILED` |
| `checkout_signature` | VARCHAR(255) | Signature from checkout popup — stored for audit |
| `created_at` | TIMESTAMPTZ | UTC creation timestamp |
| `updated_at` | TIMESTAMPTZ | UTC last update timestamp |

### `razorpay_payment_events`

Append-only log of every webhook event received. Never updated except to mark `processed = TRUE`. Serves as both audit trail and idempotency store.

| Column | Type | Description |
|---|---|---|
| `id` | BIGSERIAL | Internal surrogate key |
| `razorpay_order_id` | VARCHAR(100) | FK → razorpay_orders |
| `razorpay_payment_id` | VARCHAR(100) | Payment ID from webhook payload |
| `event_type` | VARCHAR(100) | e.g. `payment.captured`, `payment.failed` |
| `processed` | BOOLEAN | TRUE after Kafka publish succeeds |
| `raw_payload` | JSONB | Complete raw webhook body — queryable, validated on insert |
| `razorpay_event_id` | VARCHAR(100) | Unique constraint — idempotency key |
| `received_at` | TIMESTAMPTZ | UTC receipt timestamp |

**Key design decisions:**
- `amount_paise` stored as `BIGINT` — no decimal precision risk, matches Razorpay's API unit exactly
- `TIMESTAMPTZ` throughout — timezone-aware, always stored as UTC
- `JSONB` for `raw_payload` — JSON validated on insert, fields queryable without loading into Java
- `ON CONFLICT (razorpay_event_id) DO NOTHING` — duplicate webhooks silently ignored at DB level
- Conditional `WHERE` clauses on all status updates — race condition detection without distributed locks
- `CHECK` constraint on `razorpay_status` — invalid status values rejected at DB level

---

## Resilience

### Feign Client — Resilience4j

Both Razorpay API calls (`createOrder`, `capturePayment`) are wrapped with:

**Retry** — handles transient network failures:
- Max attempts: 3
- Wait between attempts: 500ms
- Retries on: `ConnectException`, `RetryableException`, `UnknownHostException`
- Does NOT retry: `RazorpayProviderException` (terminal domain errors — retrying a 4xx is never correct)

**Circuit Breaker** — protects against sustained Razorpay outages:
- Sliding window: last 10 calls
- Opens at: 50% failure rate (minimum 5 calls)
- Open state duration: 30 seconds
- Half-open probe calls: 3
- Auto-transitions from OPEN → HALF-OPEN after wait duration

**Fallback** — `CallNotPermittedException` (circuit open), `ConnectException`, and `RetryableException` are all mapped to specific `ErrorCode` values rather than generic failures. Callers always receive a structured, meaningful error response.

### Idempotency

**Webhook deduplication:** Razorpay retries webhooks using exponential backoff for up to 24 hours on failure. Every incoming webhook is inserted with `ON CONFLICT (razorpay_event_id) DO NOTHING`. If the insert returns 0 rows affected, the event is a duplicate — processing is skipped entirely and `200 OK` is returned to Razorpay.

**Status transition safety:** All `UPDATE` statements include a `WHERE razorpay_status = '<expected_current_status>'` condition. If zero rows are updated, a concurrent request already performed the transition — detected and handled without distributed locks or pessimistic locking.

---

## Security

### API Authentication (Razorpay API calls)
HTTP Basic Auth using Razorpay API key and secret, applied via a Feign `RequestInterceptor`. Credentials injected from AWS Secrets Manager at runtime — never in source code or committed configuration files.

### Checkout Signature Verification (handler path)
```
HMAC-SHA256(razorpay_order_id + "|" + razorpay_payment_id, razorpay_key_secret)
```
Verified server-side before the Capture API is called. A signature mismatch results in an immediate `401 UNAUTHORIZED` — the capture call is never made.

### Webhook Signature Verification
```
HMAC-SHA256(raw_request_body_bytes, razorpay_webhook_secret)
```
Verified before any payload parsing or database writes. Uses a **separate secret** from the API key — intentionally independent for defence in depth. Raw body bytes are used directly — parsing before verification would risk byte-sequence changes that invalidate the HMAC.

> Skipping signature verification is the most common cause of fraudulent payment confirmation. Both verification steps are mandatory and cannot be bypassed.

---

## Running Locally

### Prerequisites

- Java 17
- Maven 3.8+
- PostgreSQL 18 running locally
- Razorpay test account (Dashboard → API Keys → Test Mode)

### Setup

**1. Create the database:**
```sql
CREATE DATABASE razorpay_provider_db;
```

**2. Configure credentials in `application.properties` (local profile):**
```properties
razorpay.key.id=rzp_test_xxxxxxxxxxxx
razorpay.key.secret=your_test_key_secret
razorpay.webhook.secret=your_test_webhook_secret
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
```

**3. Build and run:**
```bash
mvn clean install
mvn spring-boot:run
```

### Testing the create order endpoint

```bash
curl -X POST http://localhost:8080/v1/internal/orders \
  -H "Content-Type: application/json" \
  -d '{"amount": 500, "currency": "INR"}'
```

### Testing webhook signature locally

Use [Razorpay's webhook simulator](https://dashboard.razorpay.com/app/webhooks) in test mode, or use `ngrok` to expose your local endpoint:

```bash
ngrok http 8080
# Update webhook URL in Razorpay Dashboard to your ngrok URL
```

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Language |
| Spring Boot | 3.x | Application framework |
| Spring JDBC | — | Database access — conditional updates, no ORM overhead |
| OpenFeign | — | Declarative HTTP client for Razorpay API |
| Resilience4j | 2.4.0 | Circuit breaker and retry for Razorpay calls |
| Apache Kafka | — | Async payment event publishing to processing service |
| PostgreSQL | 18 | Provider-specific persistence |
| HikariCP | — | JDBC connection pooling (Spring Boot default) |
| Jackson | — | JSON serialisation / webhook payload parsing |
| Lombok | — | Boilerplate reduction (`@Data`, `@Builder`, `@Slf4j`) |
| JUnit 5 | — | Unit testing |
| Mockito | — | Mocking in tests |
| Maven | 3.8+ | Build and dependency management |
| Docker | — | Containerisation |
| AWS EC2 | — | Deployment target |
| AWS Secrets Manager | — | Runtime secret injection — zero credentials in config files |
| Prometheus + Grafana | — | Metrics and dashboards |
| Loki | — | Log aggregation |

---

## Design Decisions

**Spring JDBC over JPA/Hibernate** — Payment status updates require conditional `WHERE` clauses to detect concurrent transitions without distributed locks. JPA's save/merge model does not expose row-count feedback from conditional updates. Spring JDBC's `jdbcTemplate.update()` returns affected row count directly, enabling race condition detection at the database level.

**Manual capture mode** — Auto-capture means Razorpay takes funds before your server has verified anything. Manual capture gives a deliberate window to verify the signature first. The cost is one extra API call. The benefit is a fraudulent payment can never be captured — the capture call is simply never made.

**Webhook as sole `SUCCESS` trigger** — The checkout handler path verifies and captures, but does not transition to `SUCCESS`. Only a verified `payment.captured` webhook does. This ensures your system's source of truth is always Razorpay's server-to-server confirmation, resilient to browser crashes, network drops, or client-side tampering.

**Provider-agnostic architecture** — This service is deliberately the only one that knows Razorpay exists. The processing and validation services speak in terms of generic payment events over Kafka. Adding a new provider requires only a new provider service — the rest of the system is unchanged.