package com.mysillydreams.payment.service;

import com.mysillydreams.payment.domain.InternationalPayment;
import com.mysillydreams.payment.domain.PaymentStatus;
import com.mysillydreams.payment.repository.InternationalPaymentRepository;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing international payments
 * Handles payments from international customers for treasure hunts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InternationalPaymentService {

    private final InternationalPaymentRepository internationalPaymentRepository;
    private final RazorpayClient razorpayClient;
    private final OutboxEventService outboxEventService;
    private final CurrencyConversionService currencyConversionService;

    // Supported international currencies
    private static final Map<String, String> SUPPORTED_CURRENCIES = Map.of(
        "USD", "US Dollar",
        "EUR", "Euro",
        "GBP", "British Pound",
        "AUD", "Australian Dollar",
        "CAD", "Canadian Dollar",
        "SGD", "Singapore Dollar",
        "AED", "UAE Dirham",
        "SAR", "Saudi Riyal"
    );

    /**
     * Get supported currencies for international payments
     */
    public Map<String, String> getSupportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }

    /**
     * Get currency conversion rates
     */
    public Map<String, Object> getCurrencyConversion(String fromCurrency, BigDecimal amount) {
        try {
            // Get current exchange rate
            BigDecimal exchangeRate = currencyConversionService.getExchangeRate(fromCurrency, "INR");
            BigDecimal convertedAmount = amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

            // Calculate fees (typically 2-3% for international transactions)
            BigDecimal conversionFee = amount.multiply(BigDecimal.valueOf(0.025)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalAmount = amount.add(conversionFee);
            BigDecimal totalInINR = totalAmount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

            return Map.of(
                "from_currency", fromCurrency,
                "to_currency", "INR",
                "original_amount", amount,
                "exchange_rate", exchangeRate,
                "converted_amount", convertedAmount,
                "conversion_fee", conversionFee,
                "total_amount", totalAmount,
                "total_in_inr", totalInINR,
                "rate_timestamp", System.currentTimeMillis()
            );

        } catch (Exception e) {
            log.error("Error getting currency conversion for {} {}: {}", amount, fromCurrency, e.getMessage(), e);
            throw new RuntimeException("Failed to get currency conversion: " + e.getMessage(), e);
        }
    }

    /**
     * Create international payment
     */
    @Transactional
    public InternationalPayment createInternationalPayment(UUID enrollmentId, String customerName,
                                                          String customerEmail, String customerPhone,
                                                          String country, BigDecimal amount, 
                                                          String currency, String description) {
        try {
            // Validate currency
            if (!SUPPORTED_CURRENCIES.containsKey(currency)) {
                throw new IllegalArgumentException("Unsupported currency: " + currency);
            }

            // Get currency conversion
            Map<String, Object> conversion = getCurrencyConversion(currency, amount);
            BigDecimal totalInINR = (BigDecimal) conversion.get("total_in_inr");
            BigDecimal exchangeRate = (BigDecimal) conversion.get("exchange_rate");

            // Create customer in Razorpay
            JSONObject customerRequest = new JSONObject();
            customerRequest.put("name", customerName);
            customerRequest.put("email", customerEmail);
            customerRequest.put("contact", customerPhone);
            
            // Add international customer details
            JSONObject notes = new JSONObject();
            notes.put("country", country);
            notes.put("currency", currency);
            notes.put("international_customer", "true");
            customerRequest.put("notes", notes);

            com.razorpay.Customer razorpayCustomer = razorpayClient.customers.create(customerRequest);

            // Create order for international payment
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", totalInINR.multiply(BigDecimal.valueOf(100)).intValue());
            orderRequest.put("currency", "INR"); // Razorpay processes in INR
            orderRequest.put("customer_id", razorpayCustomer.get("id"));
            orderRequest.put("description", description);

            // Add international payment notes
            JSONObject orderNotes = new JSONObject();
            orderNotes.put("enrollment_id", enrollmentId.toString());
            orderNotes.put("original_currency", currency);
            orderNotes.put("original_amount", amount.toString());
            orderNotes.put("exchange_rate", exchangeRate.toString());
            orderNotes.put("customer_country", country);
            orderNotes.put("international_payment", "true");
            orderRequest.put("notes", orderNotes);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            // Save to database
            InternationalPayment payment = InternationalPayment.builder()
                    .id(UUID.randomUUID())
                    .enrollmentId(enrollmentId)
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .razorpayCustomerId(razorpayCustomer.get("id"))
                    .customerName(customerName)
                    .customerEmail(customerEmail)
                    .customerPhone(customerPhone)
                    .customerCountry(country)
                    .originalAmount(amount)
                    .originalCurrency(currency)
                    .exchangeRate(exchangeRate)
                    .convertedAmount(totalInINR)
                    .status(PaymentStatus.CREATED)
                    .build();

            InternationalPayment savedPayment = internationalPaymentRepository.save(payment);

            // Publish event
            outboxEventService.publishEvent(
                    "InternationalPayment",
                    savedPayment.getId().toString(),
                    "international.payment.created",
                    savedPayment
            );

            log.info("Created international payment {} for enrollment {} from {} ({} {} -> {} INR)", 
                    savedPayment.getId(), enrollmentId, country, amount, currency, totalInINR);

            return savedPayment;

        } catch (Exception e) {
            log.error("Error creating international payment for enrollment {}: {}", enrollmentId, e.getMessage(), e);
            throw new RuntimeException("Failed to create international payment: " + e.getMessage(), e);
        }
    }

    /**
     * Handle international payment success
     */
    @Transactional
    public void handleInternationalPaymentSuccess(String razorpayOrderId, String razorpayPaymentId) {
        InternationalPayment payment = internationalPaymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException("International payment not found: " + razorpayOrderId));

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus(PaymentStatus.SUCCESS);
        internationalPaymentRepository.save(payment);

        // Publish success event
        outboxEventService.publishEvent(
                "InternationalPayment",
                payment.getId().toString(),
                "international.payment.success",
                payment
        );

        log.info("International payment {} completed successfully", payment.getId());
    }

    /**
     * Handle international payment failure
     */
    @Transactional
    public void handleInternationalPaymentFailure(String razorpayOrderId, String errorMessage) {
        InternationalPayment payment = internationalPaymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException("International payment not found: " + razorpayOrderId));

        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorMessage(errorMessage);
        internationalPaymentRepository.save(payment);

        // Publish failure event
        outboxEventService.publishEvent(
                "InternationalPayment",
                payment.getId().toString(),
                "international.payment.failed",
                payment
        );

        log.warn("International payment {} failed: {}", payment.getId(), errorMessage);
    }

    /**
     * Get international payment details
     */
    public Map<String, Object> getInternationalPaymentDetails(UUID paymentId) {
        InternationalPayment payment = internationalPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("International payment not found: " + paymentId));

        return Map.of(
            "payment", payment,
            "conversion_details", Map.of(
                "original_amount", payment.getOriginalAmount(),
                "original_currency", payment.getOriginalCurrency(),
                "exchange_rate", payment.getExchangeRate(),
                "converted_amount", payment.getConvertedAmount(),
                "conversion_date", payment.getCreatedAt()
            ),
            "customer_details", Map.of(
                "name", payment.getCustomerName(),
                "email", payment.getCustomerEmail(),
                "country", payment.getCustomerCountry()
            )
        );
    }

    /**
     * Get country-specific payment methods
     */
    public Map<String, Object> getCountryPaymentMethods(String country) {
        // This would return country-specific payment methods
        // For now, returning common international payment methods
        
        return Map.of(
            "country", country,
            "supported_methods", Map.of(
                "cards", Map.of(
                    "enabled", true,
                    "types", new String[]{"visa", "mastercard", "amex"},
                    "description", "International credit/debit cards"
                ),
                "digital_wallets", Map.of(
                    "enabled", true,
                    "types", new String[]{"paypal", "apple_pay", "google_pay"},
                    "description", "Digital wallet payments"
                ),
                "bank_transfers", Map.of(
                    "enabled", false,
                    "description", "Not available for international customers"
                )
            ),
            "currency_info", Map.of(
                "supported_currencies", SUPPORTED_CURRENCIES.keySet(),
                "default_currency", getDefaultCurrencyForCountry(country),
                "conversion_fee", "2.5%"
            )
        );
    }

    /**
     * Get default currency for country
     */
    private String getDefaultCurrencyForCountry(String country) {
        // Simple mapping - in production, use a comprehensive country-currency mapping
        return switch (country.toUpperCase()) {
            case "US", "USA" -> "USD";
            case "UK", "GB", "GBR" -> "GBP";
            case "AU", "AUS" -> "AUD";
            case "CA", "CAN" -> "CAD";
            case "SG", "SGP" -> "SGD";
            case "AE", "UAE" -> "AED";
            case "SA", "SAU" -> "SAR";
            default -> "USD"; // Default to USD
        };
    }
}
