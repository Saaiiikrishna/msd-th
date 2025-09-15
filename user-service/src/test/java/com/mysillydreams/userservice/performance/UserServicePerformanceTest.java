package com.mysillydreams.userservice.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysillydreams.userservice.config.TestConfig;
import com.mysillydreams.userservice.dto.UserCreateRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Performance tests for User Service.
 * Tests system performance under load and concurrent access.
 * 
 * Run with: -Dperformance.tests.enabled=true
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("performance-test")
@EnabledIfSystemProperty(named = "performance.tests.enabled", matches = "true")
class UserServicePerformanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int CONCURRENT_USERS = 10;
    private static final int REQUESTS_PER_USER = 50;
    private static final long MAX_RESPONSE_TIME_MS = 1000;
    private static final double MIN_SUCCESS_RATE = 0.95;

    @BeforeEach
    void setUp() {
        // Performance test setup
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testConcurrentUserCreation() throws Exception {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(CONCURRENT_USERS);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();

        // When
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    for (int j = 0; j < REQUESTS_PER_USER; j++) {
                        long startTime = System.currentTimeMillis();
                        
                        UserCreateRequestDto request = TestConfig.TestDataBuilder.createTestUserCreateRequest();
                        request.setEmail("user" + userId + "_" + j + "@example.com");
                        
                        try {
                            MvcResult result = mockMvc.perform(post("/api/v1/users")
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                                    .andReturn();
                            
                            long responseTime = System.currentTimeMillis() - startTime;
                            responseTimes.add(responseTime);
                            totalResponseTime.addAndGet(responseTime);
                            
                            if (result.getResponse().getStatus() == 201) {
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                            }
                            
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            long responseTime = System.currentTimeMillis() - startTime;
                            responseTimes.add(responseTime);
                            totalResponseTime.addAndGet(responseTime);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        
        // Wait for completion with timeout
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        executor.shutdown();

        // Then - Analyze results
        assertTrue(completed, "Performance test should complete within timeout");
        
        int totalRequests = successCount.get() + failureCount.get();
        double successRate = (double) successCount.get() / totalRequests;
        double averageResponseTime = (double) totalResponseTime.get() / totalRequests;
        double throughput = (double) totalRequests / (testDuration / 1000.0);
        
        // Calculate percentiles
        responseTimes.sort(Long::compareTo);
        long p50 = getPercentile(responseTimes, 0.5);
        long p95 = getPercentile(responseTimes, 0.95);
        long p99 = getPercentile(responseTimes, 0.99);
        long maxResponseTime = responseTimes.get(responseTimes.size() - 1);

        // Performance assertions
        assertTrue(successRate >= MIN_SUCCESS_RATE, 
            String.format("Success rate %.2f%% should be >= %.2f%%", successRate * 100, MIN_SUCCESS_RATE * 100));
        
        assertTrue(averageResponseTime <= MAX_RESPONSE_TIME_MS, 
            String.format("Average response time %.2fms should be <= %dms", averageResponseTime, MAX_RESPONSE_TIME_MS));
        
        assertTrue(p95 <= MAX_RESPONSE_TIME_MS * 2, 
            String.format("95th percentile %dms should be <= %dms", p95, MAX_RESPONSE_TIME_MS * 2));

        // Log performance metrics
        System.out.println("\n=== Performance Test Results ===");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Successful Requests: " + successCount.get());
        System.out.println("Failed Requests: " + failureCount.get());
        System.out.println("Success Rate: " + String.format("%.2f%%", successRate * 100));
        System.out.println("Test Duration: " + testDuration + "ms");
        System.out.println("Throughput: " + String.format("%.2f requests/sec", throughput));
        System.out.println("Average Response Time: " + String.format("%.2fms", averageResponseTime));
        System.out.println("50th Percentile: " + p50 + "ms");
        System.out.println("95th Percentile: " + p95 + "ms");
        System.out.println("99th Percentile: " + p99 + "ms");
        System.out.println("Max Response Time: " + maxResponseTime + "ms");
        System.out.println("================================\n");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testConcurrentUserRetrieval() throws Exception {
        // Given - Create some test users first
        List<String> userReferenceIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UserCreateRequestDto request = TestConfig.TestDataBuilder.createTestUserCreateRequest();
            request.setEmail("perf_user_" + i + "@example.com");
            
            MvcResult result = mockMvc.perform(post("/api/v1/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();
            
            // Extract reference ID from response (simplified)
            userReferenceIds.add("USR" + System.currentTimeMillis() + i);
        }

        // Performance test for user retrieval
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(CONCURRENT_USERS);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();

        // When
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < REQUESTS_PER_USER; j++) {
                        long startTime = System.currentTimeMillis();
                        String referenceId = userReferenceIds.get(j % userReferenceIds.size());
                        
                        try {
                            MvcResult result = mockMvc.perform(get("/api/v1/users/{referenceId}", referenceId))
                                    .andReturn();
                            
                            long responseTime = System.currentTimeMillis() - startTime;
                            responseTimes.add(responseTime);
                            
                            if (result.getResponse().getStatus() == 200) {
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                            }
                            
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            long responseTime = System.currentTimeMillis() - startTime;
                            responseTimes.add(responseTime);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start test
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        executor.shutdown();

        // Then - Analyze results
        assertTrue(completed, "Retrieval performance test should complete within timeout");
        
        int totalRequests = successCount.get() + failureCount.get();
        double successRate = (double) successCount.get() / totalRequests;
        double throughput = (double) totalRequests / (testDuration / 1000.0);
        
        // Read operations should be faster
        responseTimes.sort(Long::compareTo);
        long p95 = getPercentile(responseTimes, 0.95);
        
        assertTrue(successRate >= MIN_SUCCESS_RATE, 
            "Read operation success rate should be high");
        
        assertTrue(p95 <= MAX_RESPONSE_TIME_MS / 2, 
            "Read operations should be faster than write operations");

        System.out.println("\n=== User Retrieval Performance ===");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Success Rate: " + String.format("%.2f%%", successRate * 100));
        System.out.println("Throughput: " + String.format("%.2f requests/sec", throughput));
        System.out.println("95th Percentile: " + p95 + "ms");
        System.out.println("==================================\n");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testMemoryUsageUnderLoad() throws Exception {
        // Given
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // When - Create many users to test memory usage
        for (int i = 0; i < 1000; i++) {
            UserCreateRequestDto request = TestConfig.TestDataBuilder.createTestUserCreateRequest();
            request.setEmail("memory_test_" + i + "@example.com");
            
            mockMvc.perform(post("/api/v1/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
            
            // Force garbage collection every 100 requests
            if (i % 100 == 0) {
                System.gc();
                Thread.sleep(10);
            }
        }
        
        // Force final garbage collection
        System.gc();
        Thread.sleep(100);
        
        // Then
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        long memoryIncreasePerUser = memoryIncrease / 1000;
        
        System.out.println("\n=== Memory Usage Test ===");
        System.out.println("Initial Memory: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("Final Memory: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("Memory Increase: " + (memoryIncrease / 1024 / 1024) + " MB");
        System.out.println("Memory per User: " + (memoryIncreasePerUser / 1024) + " KB");
        System.out.println("========================\n");
        
        // Assert reasonable memory usage (less than 100MB increase for 1000 users)
        assertTrue(memoryIncrease < 100 * 1024 * 1024, 
            "Memory increase should be reasonable");
    }

    // Helper method to calculate percentiles
    private long getPercentile(List<Long> sortedList, double percentile) {
        if (sortedList.isEmpty()) return 0;
        
        int index = (int) Math.ceil(percentile * sortedList.size()) - 1;
        index = Math.max(0, Math.min(index, sortedList.size() - 1));
        
        return sortedList.get(index);
    }
}
