package com.mysillydreams.treasure.integrations.adapter;

import com.mysillydreams.treasure.integrations.port.PaymentsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(prefix="feature.integrations.payments", name="enabled", havingValue="false", matchIfMissing = true)
public class PaymentsNoopAdapter implements PaymentsPort {
    @Override
    public Optional<PaymentLink> createPaymentLink(String enrollmentId, String userId, String currency, String amount, String planId) {
        log.info("[NOOP] Create payment link enrollmentId={} userId={} currency={} amount={} planId={}",
                enrollmentId, userId, currency, amount, planId);
        return Optional.empty();
    }
}
