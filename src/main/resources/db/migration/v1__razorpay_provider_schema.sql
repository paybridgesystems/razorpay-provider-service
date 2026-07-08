-- V1__razorpay_provider_schema.sql

CREATE TABLE razorpay_orders (

    id                      BIGSERIAL PRIMARY KEY,
    internal_order_id       VARCHAR(50)     NOT NULL,
    razorpay_order_id       VARCHAR(100)    NOT NULL,
    razorpay_payment_id     VARCHAR(100)    NULL,
    amount_paise            BIGINT          NOT NULL,
    currency                VARCHAR(3)      NOT NULL,
    receipt                 VARCHAR(50)     NOT NULL,
    razorpay_status         VARCHAR(20)     NOT NULL    DEFAULT 'CREATED',
    checkout_signature      VARCHAR(255)    NULL,
    created_at              TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT uq_razorpay_order_id     UNIQUE (razorpay_order_id),
    CONSTRAINT uq_internal_order_id     UNIQUE (internal_order_id),
    CONSTRAINT uq_razorpay_payment_id   UNIQUE (razorpay_payment_id),
    CONSTRAINT chk_razorpay_status      CHECK (razorpay_status IN (
        'CREATED', 'AUTHORIZED', 'CAPTURED', 'FAILED'
    ))
);

CREATE TABLE razorpay_payment_events (

    id                      BIGSERIAL PRIMARY KEY,
    razorpay_order_id       VARCHAR(100)    NOT NULL,
    razorpay_payment_id     VARCHAR(100)    NOT NULL,
    event_type              VARCHAR(100)    NOT NULL,
    processed               BOOLEAN         NOT NULL    DEFAULT FALSE,
    raw_payload             JSONB           NOT NULL,
    razorpay_event_id       VARCHAR(100)    NOT NULL,
    received_at             TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT uq_razorpay_event_id     UNIQUE (razorpay_event_id),
    CONSTRAINT fk_events_order_id       FOREIGN KEY (razorpay_order_id)
                                        REFERENCES razorpay_orders (razorpay_order_id)
);

CREATE INDEX idx_orders_razorpay_order_id
    ON razorpay_orders (razorpay_order_id);

CREATE INDEX idx_orders_internal_order_id
    ON razorpay_orders (internal_order_id);

CREATE INDEX idx_events_razorpay_event_id
    ON razorpay_payment_events (razorpay_event_id);

CREATE INDEX idx_events_order_id
    ON razorpay_payment_events (razorpay_order_id);