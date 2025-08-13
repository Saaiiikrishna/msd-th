package com.mysillydreams.payment.domain;

import lombok.Data;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Wrapper class for Razorpay Payout response
 * Since the Razorpay Java SDK might not have direct Payout class support,
 * we create our own wrapper to handle payout responses
 */
@Data
public class RazorpayPayout {
    private String id;
    private String entity;
    private String fundAccountId;
    private BigDecimal amount;
    private String currency;
    private String notes;
    private String fees;
    private String tax;
    private String status;
    private String purpose;
    private String utr;
    private String mode;
    private String referenceId;
    private String narration;
    private OffsetDateTime createdAt;
    private OffsetDateTime processedAt;
    private String failureReason;
    
    /**
     * Create RazorpayPayout from JSONObject response
     */
    public static RazorpayPayout fromJson(JSONObject json) {
        RazorpayPayout payout = new RazorpayPayout();
        payout.setId(json.optString("id"));
        payout.setEntity(json.optString("entity"));
        payout.setFundAccountId(json.optString("fund_account_id"));
        payout.setAmount(BigDecimal.valueOf(json.optLong("amount", 0)).divide(BigDecimal.valueOf(100)));
        payout.setCurrency(json.optString("currency"));
        payout.setStatus(json.optString("status"));
        payout.setPurpose(json.optString("purpose"));
        payout.setUtr(json.optString("utr"));
        payout.setMode(json.optString("mode"));
        payout.setReferenceId(json.optString("reference_id"));
        payout.setNarration(json.optString("narration"));
        payout.setFailureReason(json.optString("failure_reason"));
        
        // Handle nested objects
        if (json.has("notes")) {
            payout.setNotes(json.getJSONObject("notes").toString());
        }
        
        // Handle timestamps
        if (json.has("created_at")) {
            payout.setCreatedAt(OffsetDateTime.ofInstant(
                    Instant.ofEpochSecond(json.getLong("created_at")),
                    OffsetDateTime.now().getOffset()));
        }

        if (json.has("processed_at") && !json.isNull("processed_at")) {
            payout.setProcessedAt(OffsetDateTime.ofInstant(
                    Instant.ofEpochSecond(json.getLong("processed_at")),
                    OffsetDateTime.now().getOffset()));
        }
        
        return payout;
    }
    
    /**
     * Convert to JSONObject for API requests
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("fund_account_id", fundAccountId);
        json.put("amount", amount.multiply(BigDecimal.valueOf(100)).longValue()); // Convert to paise
        json.put("currency", currency);
        json.put("mode", mode);
        json.put("purpose", purpose);
        json.put("reference_id", referenceId);
        json.put("narration", narration);
        
        if (notes != null) {
            try {
                json.put("notes", new JSONObject(notes));
            } catch (Exception e) {
                // If notes is not valid JSON, create a simple object
                JSONObject notesObj = new JSONObject();
                notesObj.put("description", notes);
                json.put("notes", notesObj);
            }
        }
        
        return json;
    }
}
