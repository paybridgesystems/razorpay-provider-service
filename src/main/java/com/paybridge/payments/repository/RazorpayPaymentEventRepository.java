package com.paybridge.payments.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.paybridge.payments.repository.entity.RazorpayPaymentEventEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RazorpayPaymentEventRepository {

	private final JdbcTemplate jdbcTemplate;

	public boolean insertIfNotExists(RazorpayPaymentEventEntity event) {
		log.info("Inserting payment event: {} for order: {}",
				event.getEventType(), event.getRazorpayOrderId());

		String sql = """
				INSERT INTO razorpay_payment_events
				    (razorpay_order_id, razorpay_payment_id, event_type,
				     raw_payload, razorpay_event_id)
				VALUES
				    (?, ?, ?, ?::jsonb, ?)
				ON CONFLICT (razorpay_event_id) DO NOTHING
				""";

		int rowsAffected = jdbcTemplate.update(sql,
				event.getRazorpayOrderId(),
				event.getRazorpayPaymentId(),
				event.getEventType(),
				event.getRawPayload(),
				event.getRazorpayEventId()
				);

		return rowsAffected > 0;
	}

	public void markProcessed(String razorpayEventId) {
		log.info("Marking event as processed: {}", razorpayEventId);

		String sql = """
				UPDATE razorpay_payment_events
				SET processed = TRUE
				WHERE razorpay_event_id = ?
				""";

		jdbcTemplate.update(sql, razorpayEventId);
	}
}