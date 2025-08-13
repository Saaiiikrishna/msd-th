package com.mysillydreams.payment.controller;

import com.mysillydreams.payment.domain.Invoice;
import com.mysillydreams.payment.dto.TreasureEnrollmentEvent;
import com.mysillydreams.payment.service.InvoiceService;
import com.mysillydreams.payment.service.TreasurePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments/v1/treasure")
@RequiredArgsConstructor
@Tag(name = "Treasure Hunt Payments", description = "Payment processing for treasure hunt enrollments")
public class TreasurePaymentController {

    private final TreasurePaymentService treasurePaymentService;
    private final InvoiceService invoiceService;

    @Operation(
            summary = "Process treasure hunt enrollment payment",
            description = "Creates invoice and initiates payment process for treasure hunt enrollment"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment initiated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TreasurePaymentService.TreasurePaymentResult.class))),
            @ApiResponse(responseCode = "400", description = "Invalid enrollment data"),
            @ApiResponse(responseCode = "500", description = "Payment processing failed")
    })
    @PostMapping("/process-enrollment")
    public ResponseEntity<TreasurePaymentService.TreasurePaymentResult> processEnrollmentPayment(
            @Parameter(description = "Treasure hunt enrollment details", required = true)
            @RequestBody TreasureEnrollmentEvent enrollmentEvent) {
        
        TreasurePaymentService.TreasurePaymentResult result = 
                treasurePaymentService.processEnrollmentPayment(enrollmentEvent);
        
        if (result.success()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @Operation(
            summary = "Handle payment success webhook",
            description = "Webhook endpoint for Razorpay payment success notifications"
    )
    @PostMapping("/webhook/payment-success")
    public ResponseEntity<String> handlePaymentSuccess(
            @Parameter(description = "Razorpay order ID", required = true)
            @RequestParam String razorpayOrderId,
            @Parameter(description = "Razorpay payment ID", required = true)
            @RequestParam String razorpayPaymentId,
            @Parameter(description = "Payment method used", required = true)
            @RequestParam String paymentMethod) {
        
        try {
            treasurePaymentService.handlePaymentSuccess(razorpayOrderId, razorpayPaymentId, paymentMethod);
            return ResponseEntity.ok("Payment success handled");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error handling payment success: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Handle payment failure webhook",
            description = "Webhook endpoint for Razorpay payment failure notifications"
    )
    @PostMapping("/webhook/payment-failure")
    public ResponseEntity<String> handlePaymentFailure(
            @Parameter(description = "Razorpay order ID", required = true)
            @RequestParam String razorpayOrderId,
            @Parameter(description = "Error message", required = true)
            @RequestParam String errorMessage) {
        
        try {
            treasurePaymentService.handlePaymentFailure(razorpayOrderId, errorMessage);
            return ResponseEntity.ok("Payment failure handled");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error handling payment failure: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Get invoice by enrollment ID",
            description = "Retrieves invoice details for a specific enrollment"
    )
    @GetMapping("/invoice/enrollment/{enrollmentId}")
    public ResponseEntity<Invoice> getInvoiceByEnrollmentId(
            @Parameter(description = "Enrollment ID", required = true)
            @PathVariable UUID enrollmentId) {
        
        Optional<Invoice> invoice = invoiceService.findByEnrollmentId(enrollmentId);
        return invoice.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get invoice by registration ID",
            description = "Retrieves invoice details for a specific registration ID"
    )
    @GetMapping("/invoice/registration/{registrationId}")
    public ResponseEntity<Invoice> getInvoiceByRegistrationId(
            @Parameter(description = "Registration ID (e.g., TH-0825-IND-000101)", required = true)
            @PathVariable String registrationId) {
        
        Optional<Invoice> invoice = invoiceService.findByRegistrationId(registrationId);
        return invoice.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get user invoices",
            description = "Retrieves all invoices for a specific user"
    )
    @GetMapping("/invoice/user/{userId}")
    public ResponseEntity<List<Invoice>> getUserInvoices(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId) {
        
        List<Invoice> invoices = invoiceService.findByUserId(userId);
        return ResponseEntity.ok(invoices);
    }

    @Operation(
            summary = "Get payment statistics",
            description = "Retrieves payment statistics and metrics"
    )
    @GetMapping("/statistics")
    public ResponseEntity<InvoiceService.PaymentStatistics> getPaymentStatistics() {
        InvoiceService.PaymentStatistics stats = invoiceService.getPaymentStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Get payment test page",
            description = "Serves a test page for payment link testing with dynamic payment link injection"
    )
    @GetMapping("/test-page")
    public ResponseEntity<String> getPaymentTestPage(
            @Parameter(description = "Payment link URL to test", required = false)
            @RequestParam(required = false) String paymentLink,
            @Parameter(description = "Order ID for testing", required = false)
            @RequestParam(required = false) String orderId) {

        String htmlContent = generatePaymentTestHtml(paymentLink, orderId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(htmlContent);
    }

    private String generatePaymentTestHtml(String paymentLink, String orderId) {
        // Generate dynamic HTML with payment link injection using string concatenation
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Payment Link Test - Treasure Hunt</title>\n");
        html.append("    <script src=\"https://checkout.razorpay.com/v1/checkout.js\"></script>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; background-color: #f5f5f5; }\n");
        html.append("        .container { background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n");
        html.append("        .header { text-align: center; color: #333; margin-bottom: 30px; }\n");
        html.append("        .payment-link-section { background: #e8f4fd; padding: 20px; border-radius: 8px; margin-bottom: 20px; }\n");
        html.append("        .payment-link { background: white; padding: 15px; border-radius: 5px; word-break: break-all; font-family: monospace; border: 1px solid #ddd; }\n");
        html.append("        .test-button { background: #3399cc; color: white; border: none; padding: 15px 30px; font-size: 18px; border-radius: 5px; cursor: pointer; width: 100%; margin-top: 20px; }\n");
        html.append("        .test-button:hover { background: #2980b9; }\n");
        html.append("        .test-button:disabled { background: #ccc; cursor: not-allowed; }\n");
        html.append("        .test-cards { background: #e8f4fd; padding: 20px; border-radius: 8px; margin-top: 20px; }\n");
        html.append("        .card-info { background: white; padding: 10px; margin: 10px 0; border-radius: 5px; border-left: 4px solid #3399cc; }\n");
        html.append("        .status { padding: 15px; border-radius: 5px; margin-top: 20px; display: none; }\n");
        html.append("        .success { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }\n");
        html.append("        .error { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }\n");
        html.append("        .info { background: #d1ecf1; color: #0c5460; border: 1px solid #bee5eb; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"container\">\n");
        html.append("        <div class=\"header\">\n");
        html.append("            <h1>🔗 Payment Link Tester</h1>\n");
        html.append("            <p>Test Razorpay payment links with test card details</p>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"payment-link-section\">\n");
        html.append("            <h3>Payment Link</h3>\n");
        html.append("            <div class=\"payment-link\" id=\"paymentLinkDisplay\">\n");
        html.append("                ").append(paymentLink != null ? paymentLink : "No payment link provided").append("\n");
        html.append("            </div>\n");
        html.append("            <p><strong>Order ID:</strong> <span id=\"orderIdDisplay\">").append(orderId != null ? orderId : "Not provided").append("</span></p>\n");
        html.append("        </div>\n");
        html.append("        <button class=\"test-button\" onclick=\"testPaymentLink()\"").append(paymentLink == null ? " disabled" : "").append(">\n");
        html.append("            🧪 Test Payment Link\n");
        html.append("        </button>\n");
        html.append("        <div class=\"test-cards\">\n");
        html.append("            <h3>🧪 Test Card Details</h3>\n");
        html.append("            <div class=\"card-info\">\n");
        html.append("                <strong>VISA Success:</strong> 4111 1111 1111 1111<br>\n");
        html.append("                <strong>CVV:</strong> Any 3 digits | <strong>Expiry:</strong> Any future date\n");
        html.append("            </div>\n");
        html.append("            <div class=\"card-info\">\n");
        html.append("                <strong>Mastercard Success:</strong> 5555 5555 5555 4444<br>\n");
        html.append("                <strong>CVV:</strong> Any 3 digits | <strong>Expiry:</strong> Any future date\n");
        html.append("            </div>\n");
        html.append("            <div class=\"card-info\">\n");
        html.append("                <strong>UPI Success:</strong> success@razorpay<br>\n");
        html.append("                <strong>Note:</strong> Use this UPI ID for successful UPI payments\n");
        html.append("            </div>\n");
        html.append("            <div class=\"card-info\">\n");
        html.append("                <strong>Card Failure:</strong> 4000 0000 0000 0002<br>\n");
        html.append("                <strong>Note:</strong> This card will simulate payment failure\n");
        html.append("            </div>\n");
        html.append("        </div>\n");
        html.append("        <div id=\"status\" class=\"status\"></div>\n");
        html.append("    </div>\n");
        html.append("    <script>\n");
        html.append("        const PAYMENT_LINK = '").append(paymentLink != null ? paymentLink : "").append("';\n");
        html.append("        const ORDER_ID = '").append(orderId != null ? orderId : "").append("';\n");
        html.append("        function testPaymentLink() {\n");
        html.append("            if (!PAYMENT_LINK) { showStatus('No payment link provided', 'error'); return; }\n");
        html.append("            try {\n");
        html.append("                showStatus('Opening payment link...', 'info');\n");
        html.append("                if (PAYMENT_LINK.includes('checkout.js')) {\n");
        html.append("                    const urlParams = new URLSearchParams(PAYMENT_LINK.split('?')[1]);\n");
        html.append("                    const orderIdFromLink = urlParams.get('order_id');\n");
        html.append("                    if (orderIdFromLink) { openRazorpayCheckout(orderIdFromLink); }\n");
        html.append("                    else { showStatus('Invalid payment link format', 'error'); }\n");
        html.append("                } else { window.open(PAYMENT_LINK, '_blank'); showStatus('Payment link opened in new window', 'info'); }\n");
        html.append("            } catch (error) { showStatus('Error: ' + error.message, 'error'); }\n");
        html.append("        }\n");
        html.append("        function openRazorpayCheckout(orderId) {\n");
        html.append("            const options = { key: 'rzp_test_fQluXDFtJjH3vV', order_id: orderId,\n");
        html.append("                handler: function (response) { showStatus('🎉 Payment Successful!<br><strong>Payment ID:</strong> ' + response.razorpay_payment_id + '<br><strong>Order ID:</strong> ' + response.razorpay_order_id, 'success'); },\n");
        html.append("                modal: { ondismiss: function() { showStatus('Payment cancelled by user', 'error'); } }, theme: { color: '#3399cc' } };\n");
        html.append("            const rzp = new Razorpay(options);\n");
        html.append("            rzp.on('payment.failed', function (response) { showStatus('❌ Payment Failed!<br><strong>Reason:</strong> ' + (response.error ? response.error.description : 'Payment failed'), 'error'); });\n");
        html.append("            rzp.open();\n");
        html.append("        }\n");
        html.append("        function showStatus(message, type) { const statusDiv = document.getElementById('status'); statusDiv.innerHTML = message; statusDiv.className = 'status ' + type; statusDiv.style.display = 'block'; }\n");
        html.append("    </script>\n");
        html.append("</body>\n");
        html.append("</html>");
        return html.toString();
    }
}
