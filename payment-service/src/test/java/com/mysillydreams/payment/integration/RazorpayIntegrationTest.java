package com.mysillydreams.payment.integration;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Razorpay payment flows using actual test credentials.
 * This test verifies that the payment service can successfully interact with Razorpay's sandbox environment.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "payment.razorpay.key-id=rzp_test_fQluXDFtJjH3vV",
    "payment.razorpay.key-secret=aBO9FVztNddcnjnoosqVrmHO",
    "payment.razorpay.webhook.secret=treasure_hunt_webhook_secret_2025"
})
public class RazorpayIntegrationTest {

    @Autowired
    private RazorpayClient razorpayClient;

    @Test
    public void testRazorpayClientConfiguration() {
        assertNotNull(razorpayClient, "RazorpayClient should be configured");
    }

    @Test
    public void testCreateRazorpayOrder() throws RazorpayException {
        // Test creating an order directly with Razorpay
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", 50000); // ₹500 in paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "test_receipt_" + System.currentTimeMillis());

        Order order = razorpayClient.orders.create(orderRequest);

        assertNotNull(order);
        assertNotNull(order.get("id"));
        assertEquals("INR", order.get("currency"));
        assertEquals(50000, ((Integer) order.get("amount")).intValue());
        assertEquals("created", order.get("status"));
        
        System.out.println("✅ Successfully created Razorpay order: " + order.get("id"));
        System.out.println("Order details: " + order.toString());
    }

    @Test
    public void testRazorpayConfiguration() {
        // Test that the Razorpay client is properly configured
        assertNotNull(razorpayClient, "RazorpayClient should be configured");
        System.out.println("✅ RazorpayClient is properly configured");
    }

    @Test
    public void testRazorpayOrderCreationWithDifferentAmounts() throws RazorpayException {
        // Test creating orders with different amounts
        int[] testAmounts = {1000, 5000, 10000, 50000}; // ₹10, ₹50, ₹100, ₹500
        
        for (int amount : testAmounts) {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "test_amount_" + amount + "_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(orderRequest);

            assertNotNull(order);
            assertEquals(amount, ((Integer) order.get("amount")).intValue());
            assertEquals("created", order.get("status"));
            
            System.out.println("✅ Created order for ₹" + (amount/100.0) + " with ID: " + order.get("id"));
        }
    }

    @Test
    public void testRazorpayOrderWithNotes() throws RazorpayException {
        // Test creating an order with notes (metadata)
        JSONObject notes = new JSONObject();
        notes.put("treasure_id", UUID.randomUUID().toString());
        notes.put("enrollment_id", UUID.randomUUID().toString());
        notes.put("user_id", "test_user_123");
        
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", 25000); // ₹250
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "test_notes_" + System.currentTimeMillis());
        orderRequest.put("notes", notes);

        Order order = razorpayClient.orders.create(orderRequest);

        assertNotNull(order);
        assertNotNull(order.get("notes"));
        
        System.out.println("✅ Created order with notes: " + order.get("id"));
        System.out.println("Notes: " + order.get("notes"));
    }
}
