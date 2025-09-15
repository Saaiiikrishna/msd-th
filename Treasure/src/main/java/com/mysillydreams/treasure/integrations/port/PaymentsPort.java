package com.mysillydreams.treasure.integrations.port;

import java.util.Optional;

public interface PaymentsPort {
    record PaymentLink(String link, String paymentId) {}
    Optional<PaymentLink> createPaymentLink(String enrollmentId, String userId, String currency, String amount, String planId);
}
