package com.mysillydreams.payment.repository;

import com.mysillydreams.payment.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransaction, UUID> {

    // Find by enrollment ID (if one enrollment can have multiple payment attempts, this might return a list)
    Optional<PaymentTransaction> findByEnrollmentId(UUID enrollmentId); // Assuming one primary transaction per enrollment for now

    // Find by Razorpay Payment ID (useful for webhook processing)
    Optional<PaymentTransaction> findByRazorpayPaymentId(String razorpayPaymentId);

    // Find by Razorpay Order ID
    Optional<PaymentTransaction> findByRazorpayOrderId(String razorpayOrderId);

    // Add other query methods as needed, e.g., finding transactions by status, date range, etc.
    // List<PaymentTransaction> findByStatus(String status);
}
