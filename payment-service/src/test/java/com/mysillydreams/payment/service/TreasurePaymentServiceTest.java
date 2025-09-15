package com.mysillydreams.payment.service;

import com.mysillydreams.payment.domain.EnrollmentType;
import com.mysillydreams.payment.domain.Invoice;
import com.mysillydreams.payment.domain.PaymentStatus;
import com.mysillydreams.payment.domain.PaymentTransaction;
import com.mysillydreams.payment.dto.TreasureEnrollmentEvent;
import com.mysillydreams.payment.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreasurePaymentServiceTest {

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private VendorPayoutService vendorPayoutService;

    @InjectMocks
    private TreasurePaymentService treasurePaymentService;

    private TreasureEnrollmentEvent enrollmentEvent;
    private Invoice testInvoice;
    private PaymentTransaction testTransaction;

    @BeforeEach
    void setUp() {
        UUID enrollmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        enrollmentEvent = new TreasureEnrollmentEvent(
                enrollmentId,
                "TH-0825-IND-000101",
                userId,
                planId,
                "Beginner Treasure Hunt",
                EnrollmentType.INDIVIDUAL,
                null,
                null,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(180),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(1150),
                "INR",
                "SAVE10",
                "Summer Sale",
                "John Doe",
                "john@example.com",
                "+91-9876543210",
                "123 Main St, Mumbai",
                UUID.randomUUID(), // vendorId
                BigDecimal.valueOf(5.0) // vendorCommissionRate
        );

        testInvoice = Invoice.builder()
                .id(UUID.randomUUID())
                .invoiceNumber("INV-2025-000001")
                .enrollmentId(enrollmentId)
                .registrationId("TH-0825-IND-000101")
                .userId(userId)
                .planId(planId)
                .totalAmount(BigDecimal.valueOf(1150))
                .currency("INR")
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        testTransaction = new PaymentTransaction(
                enrollmentId,
                BigDecimal.valueOf(1150),
                "INR",
                "PENDING",
                null,
                null
        );
    }

    @Test
    void processEnrollmentPayment_ShouldCreateInvoiceAndTransaction() throws Exception {
        // Given
        when(invoiceService.generateInvoice(any(), any())).thenReturn(testInvoice);
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(testTransaction);
        
        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_test123");
        when(razorpayClient.Orders.create(any())).thenReturn(mockOrder);
        
        when(invoiceService.updateWithPaymentTransaction(any(), any(), any())).thenReturn(testInvoice);

        // When
        TreasurePaymentService.TreasurePaymentResult result = 
                treasurePaymentService.processEnrollmentPayment(enrollmentEvent);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.invoice()).isEqualTo(testInvoice);
        assertThat(result.transaction()).isEqualTo(testTransaction);
        assertThat(result.razorpayOrderId()).isEqualTo("order_test123");
        
        verify(invoiceService).generateInvoice(any(), any());
        verify(paymentRepository, times(2)).save(any(PaymentTransaction.class));
        verify(razorpayClient.Orders).create(any());
    }

    @Test
    void processEnrollmentPayment_ShouldHandleRazorpayException() throws Exception {
        // Given
        when(invoiceService.generateInvoice(any(), any())).thenReturn(testInvoice);
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(testTransaction);
        when(razorpayClient.Orders.create(any())).thenThrow(new RuntimeException("Razorpay error"));

        // When
        TreasurePaymentService.TreasurePaymentResult result = 
                treasurePaymentService.processEnrollmentPayment(enrollmentEvent);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Razorpay error");
        
        verify(invoiceService).generateInvoice(any(), any());
        verify(paymentRepository).save(any(PaymentTransaction.class));
    }

    @Test
    void handlePaymentSuccess_ShouldUpdateInvoiceAndTransaction() {
        // Given
        String razorpayOrderId = "order_test123";
        String razorpayPaymentId = "pay_test456";
        String paymentMethod = "card";

        testInvoice.setPaymentTransactionId(testTransaction.getId());
        testInvoice.setRazorpayOrderId(razorpayOrderId);

        when(invoiceService.findByRazorpayOrderId(razorpayOrderId))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceService.markAsPaid(any(), eq(paymentMethod), eq(razorpayPaymentId)))
                .thenReturn(testInvoice);
        when(paymentRepository.findById(testTransaction.getId()))
                .thenReturn(Optional.of(testTransaction));
        when(paymentRepository.save(any(PaymentTransaction.class)))
                .thenReturn(testTransaction);

        // When
        treasurePaymentService.handlePaymentSuccess(razorpayOrderId, razorpayPaymentId, paymentMethod);

        // Then
        verify(invoiceService).markAsPaid(testInvoice.getId(), paymentMethod, razorpayPaymentId);
        verify(paymentRepository).save(argThat(transaction -> 
                "CAPTURED".equals(transaction.getStatus()) &&
                razorpayPaymentId.equals(transaction.getRazorpayPaymentId())
        ));
        verify(outboxEventService).publish(
                eq("TreasurePayment"),
                eq(testInvoice.getId().toString()),
                eq("treasure.payment.succeeded"),
                any()
        );
    }

    @Test
    void handlePaymentFailure_ShouldUpdateInvoiceAndTransaction() {
        // Given
        String razorpayOrderId = "order_test123";
        String errorMessage = "Payment failed due to insufficient funds";

        testInvoice.setPaymentTransactionId(testTransaction.getId());
        testInvoice.setRazorpayOrderId(razorpayOrderId);

        when(invoiceService.findByRazorpayOrderId(razorpayOrderId))
                .thenReturn(Optional.of(testInvoice));
        when(invoiceService.markAsFailed(testInvoice.getId()))
                .thenReturn(testInvoice);
        when(paymentRepository.findById(testTransaction.getId()))
                .thenReturn(Optional.of(testTransaction));
        when(paymentRepository.save(any(PaymentTransaction.class)))
                .thenReturn(testTransaction);

        // When
        treasurePaymentService.handlePaymentFailure(razorpayOrderId, errorMessage);

        // Then
        verify(invoiceService).markAsFailed(testInvoice.getId());
        verify(paymentRepository).save(argThat(transaction -> 
                "FAILED".equals(transaction.getStatus()) &&
                errorMessage.equals(transaction.getErrorMessage())
        ));
        verify(outboxEventService).publish(
                eq("TreasurePayment"),
                eq(testInvoice.getId().toString()),
                eq("treasure.payment.failed"),
                any()
        );
    }

    @Test
    void processEnrollmentPayment_ShouldCreateCorrectRazorpayOrder() throws Exception {
        // Given
        when(invoiceService.generateInvoice(any(), any())).thenReturn(testInvoice);
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(testTransaction);
        
        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_test123");
        when(razorpayClient.Orders.create(any())).thenReturn(mockOrder);
        
        when(invoiceService.updateWithPaymentTransaction(any(), any(), any())).thenReturn(testInvoice);

        // When
        treasurePaymentService.processEnrollmentPayment(enrollmentEvent);

        // Then
        verify(razorpayClient.Orders).create(argThat(orderRequest -> {
            org.json.JSONObject json = (org.json.JSONObject) orderRequest;
            return json.getLong("amount") == 115000L && // 1150 * 100 (paise)
                   "INR".equals(json.getString("currency")) &&
                   "INV-2025-000001".equals(json.getString("receipt")) &&
                   json.getInt("payment_capture") == 1;
        }));
    }

    @Test
    void processEnrollmentPayment_ShouldHandleTeamEnrollment() throws Exception {
        // Given
        TreasureEnrollmentEvent teamEvent = new TreasureEnrollmentEvent(
                UUID.randomUUID(),
                "TH-0825-TEAM-000101",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Team Treasure Hunt",
                EnrollmentType.TEAM,
                "Team Alpha",
                5,
                BigDecimal.valueOf(2000),
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(360),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(40),
                BigDecimal.valueOf(2300),
                "INR",
                null,
                null,
                "Team Leader",
                "leader@example.com",
                "+91-9876543210",
                "Team Address"
        );

        Invoice teamInvoice = Invoice.builder()
                .id(UUID.randomUUID())
                .enrollmentType(com.mysillydreams.payment.domain.EnrollmentType.TEAM)
                .teamName("Team Alpha")
                .teamSize(5)
                .totalAmount(BigDecimal.valueOf(2300))
                .build();

        when(invoiceService.generateInvoice(any(), any())).thenReturn(teamInvoice);
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(testTransaction);
        
        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_team123");
        when(razorpayClient.Orders.create(any())).thenReturn(mockOrder);
        
        when(invoiceService.updateWithPaymentTransaction(any(), any(), any())).thenReturn(teamInvoice);

        // When
        TreasurePaymentService.TreasurePaymentResult result = 
                treasurePaymentService.processEnrollmentPayment(teamEvent);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.invoice().getEnrollmentType()).isEqualTo(com.mysillydreams.payment.domain.EnrollmentType.TEAM);
        assertThat(result.invoice().getTeamName()).isEqualTo("Team Alpha");
        assertThat(result.invoice().getTeamSize()).isEqualTo(5);
        
        verify(razorpayClient.Orders).create(argThat(orderRequest -> {
            org.json.JSONObject json = (org.json.JSONObject) orderRequest;
            org.json.JSONObject notes = json.getJSONObject("notes");
            return json.getLong("amount") == 230000L && // 2300 * 100 (paise)
                   "Team Alpha".equals(notes.getString("team_name"));
        }));
    }
}
