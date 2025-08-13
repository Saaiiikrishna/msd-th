package com.mysillydreams.payment.service;

import com.mysillydreams.payment.domain.Subscription;
import com.mysillydreams.payment.domain.SubscriptionStatus;
import com.mysillydreams.payment.domain.SubscriptionPlan;
import com.mysillydreams.payment.repository.SubscriptionRepository;
import com.mysillydreams.payment.repository.SubscriptionPlanRepository;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing Razorpay Subscriptions
 * Handles recurring payments for treasure hunt memberships
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final RazorpayClient razorpayClient;
    private final OutboxEventService outboxEventService;

    /**
     * Create subscription plan in Razorpay
     */
    @Transactional
    public SubscriptionPlan createSubscriptionPlan(String planName, String description, 
                                                   BigDecimal amount, String interval, 
                                                   int intervalCount) {
        try {
            // Create plan in Razorpay
            JSONObject planRequest = new JSONObject();
            planRequest.put("period", interval.toLowerCase()); // daily, weekly, monthly, yearly
            planRequest.put("interval", intervalCount);
            planRequest.put("item", new JSONObject()
                    .put("name", planName)
                    .put("description", description)
                    .put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue())
                    .put("currency", "INR"));

            com.razorpay.Plan razorpayPlan = razorpayClient.plans.create(planRequest);

            // Save to database
            SubscriptionPlan plan = SubscriptionPlan.builder()
                    .id(UUID.randomUUID())
                    .razorpayPlanId(razorpayPlan.get("id"))
                    .planName(planName)
                    .description(description)
                    .amount(amount)
                    .currency("INR")
                    .interval(interval)
                    .intervalCount(intervalCount)
                    .isActive(true)
                    .build();

            SubscriptionPlan savedPlan = subscriptionPlanRepository.save(plan);

            log.info("Created subscription plan {} with Razorpay plan ID: {}", 
                    savedPlan.getId(), savedPlan.getRazorpayPlanId());

            return savedPlan;

        } catch (Exception e) {
            log.error("Error creating subscription plan: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create subscription plan: " + e.getMessage(), e);
        }
    }

    /**
     * Create subscription for user
     */
    @Transactional
    public Subscription createSubscription(UUID userId, UUID planId, String customerEmail, 
                                         String customerName, String customerPhone) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found: " + planId));

        try {
            // Create customer if not exists
            JSONObject customerRequest = new JSONObject();
            customerRequest.put("name", customerName);
            customerRequest.put("email", customerEmail);
            customerRequest.put("contact", customerPhone);

            com.razorpay.Customer razorpayCustomer = razorpayClient.customers.create(customerRequest);

            // Create subscription
            JSONObject subscriptionRequest = new JSONObject();
            subscriptionRequest.put("plan_id", plan.getRazorpayPlanId());
            subscriptionRequest.put("customer_id", razorpayCustomer.get("id"));
            subscriptionRequest.put("total_count", 12); // 12 cycles by default
            subscriptionRequest.put("quantity", 1);

            // Add notes
            JSONObject notes = new JSONObject();
            notes.put("user_id", userId.toString());
            notes.put("plan_name", plan.getPlanName());
            subscriptionRequest.put("notes", notes);

            com.razorpay.Subscription razorpaySubscription = razorpayClient.subscriptions.create(subscriptionRequest);

            // Save to database
            Subscription subscription = Subscription.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .planId(planId)
                    .razorpaySubscriptionId(razorpaySubscription.get("id"))
                    .razorpayCustomerId(razorpayCustomer.get("id"))
                    .status(SubscriptionStatus.CREATED)
                    .currentPeriodStart(LocalDateTime.now())
                    .currentPeriodEnd(LocalDateTime.now().plusMonths(1)) // Default monthly
                    .build();

            Subscription savedSubscription = subscriptionRepository.save(subscription);

            // Publish event
            outboxEventService.publishEvent(
                    "Subscription",
                    savedSubscription.getId().toString(),
                    "subscription.created",
                    savedSubscription
            );

            log.info("Created subscription {} for user {} with plan {}", 
                    savedSubscription.getId(), userId, plan.getPlanName());

            return savedSubscription;

        } catch (Exception e) {
            log.error("Error creating subscription for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel subscription
     */
    @Transactional
    public void cancelSubscription(UUID subscriptionId, boolean cancelAtCycleEnd) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        try {
            // Cancel in Razorpay
            com.razorpay.Subscription razorpaySubscription = razorpayClient.subscriptions
                    .fetch(subscription.getRazorpaySubscriptionId());
            
            JSONObject cancelRequest = new JSONObject();
            cancelRequest.put("cancel_at_cycle_end", cancelAtCycleEnd);
            razorpaySubscription.cancel(cancelRequest);

            // Update status
            subscription.setStatus(cancelAtCycleEnd ? SubscriptionStatus.PENDING_CANCELLATION : SubscriptionStatus.CANCELLED);
            if (!cancelAtCycleEnd) {
                subscription.setCancelledAt(LocalDateTime.now());
            }
            subscriptionRepository.save(subscription);

            // Publish event
            outboxEventService.publishEvent(
                    "Subscription",
                    subscription.getId().toString(),
                    "subscription.cancelled",
                    subscription
            );

            log.info("Cancelled subscription {} (cancel at cycle end: {})", subscriptionId, cancelAtCycleEnd);

        } catch (Exception e) {
            log.error("Error cancelling subscription {}: {}", subscriptionId, e.getMessage(), e);
            throw new RuntimeException("Failed to cancel subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Pause subscription
     */
    @Transactional
    public void pauseSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        try {
            // Pause in Razorpay
            com.razorpay.Subscription razorpaySubscription = razorpayClient.subscriptions
                    .fetch(subscription.getRazorpaySubscriptionId());
            razorpaySubscription.pause();

            // Update status
            subscription.setStatus(SubscriptionStatus.PAUSED);
            subscriptionRepository.save(subscription);

            // Publish event
            outboxEventService.publishEvent(
                    "Subscription",
                    subscription.getId().toString(),
                    "subscription.paused",
                    subscription
            );

            log.info("Paused subscription {}", subscriptionId);

        } catch (Exception e) {
            log.error("Error pausing subscription {}: {}", subscriptionId, e.getMessage(), e);
            throw new RuntimeException("Failed to pause subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Resume subscription
     */
    @Transactional
    public void resumeSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        try {
            // Resume in Razorpay
            com.razorpay.Subscription razorpaySubscription = razorpayClient.subscriptions
                    .fetch(subscription.getRazorpaySubscriptionId());
            razorpaySubscription.resume();

            // Update status
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(subscription);

            // Publish event
            outboxEventService.publishEvent(
                    "Subscription",
                    subscription.getId().toString(),
                    "subscription.resumed",
                    subscription
            );

            log.info("Resumed subscription {}", subscriptionId);

        } catch (Exception e) {
            log.error("Error resuming subscription {}: {}", subscriptionId, e.getMessage(), e);
            throw new RuntimeException("Failed to resume subscription: " + e.getMessage(), e);
        }
    }
}
