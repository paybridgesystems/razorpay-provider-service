package com.paybridge.payments.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.paybridge.payments.repository.entity.RazorpayOrderEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RazorpayOrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public void insert(RazorpayOrderEntity order) {
        log.info("Inserting razorpay order: {}", order.getRazorpayOrderId());

        String sql = """
            INSERT INTO razorpay_orders
                (internal_order_id, razorpay_order_id, amount_paise,
                 currency, receipt, razorpay_status)
            VALUES
                (?, ?, ?, ?, ?, 'CREATED')
            """;

        jdbcTemplate.update(sql,
            order.getInternalOrderId(),
            order.getRazorpayOrderId(),
            order.getAmountPaise(),
            order.getCurrency(),
            order.getReceipt()
        );
    }

    public int updateToAuthorized(String razorpayOrderId,
                                   String razorpayPaymentId,
                                   String checkoutSignature) {
        log.info("Updating order to AUTHORIZED: {}", razorpayOrderId);

        String sql = """
            UPDATE razorpay_orders
            SET razorpay_payment_id = ?,
                checkout_signature  = ?,
                razorpay_status     = 'AUTHORIZED',
                updated_at          = NOW()
            WHERE razorpay_order_id = ?
            AND   razorpay_status   = 'CREATED'
            """;

        return jdbcTemplate.update(sql,
            razorpayPaymentId,
            checkoutSignature,
            razorpayOrderId
        );
    }

    public int updateToCaptured(String razorpayOrderId) {
        log.info("Updating order to CAPTURED: {}", razorpayOrderId);

        String sql = """
            UPDATE razorpay_orders
            SET razorpay_status = 'CAPTURED',
                updated_at      = NOW()
            WHERE razorpay_order_id = ?
            AND   razorpay_status   = 'AUTHORIZED'
            """;

        return jdbcTemplate.update(sql, razorpayOrderId);
    }

    public int updateToFailed(String razorpayOrderId) {
        log.info("Updating order to FAILED: {}", razorpayOrderId);

        String sql = """
            UPDATE razorpay_orders
            SET razorpay_status = 'FAILED',
                updated_at      = NOW()
            WHERE razorpay_order_id = ?
            AND   razorpay_status NOT IN ('CAPTURED')
            """;

        return jdbcTemplate.update(sql, razorpayOrderId);
    }
}
