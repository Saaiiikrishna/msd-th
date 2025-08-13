package com.mysillydreams.treasure.api.grpc.client;

import com.mysillydreams.payments.v1.CreatePaymentLinkRequest;
import com.mysillydreams.payments.v1.CreatePaymentLinkResponse;
import com.mysillydreams.payments.v1.PaymentsServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentsGrpcClient {

    @GrpcClient("payments")
    private PaymentsServiceGrpc.PaymentsServiceBlockingStub stub;

    public CreatePaymentLinkResponse createLink(String enrollmentId, String userId,
                                                String currency, String amount,
                                                String planId) {
        try {
            return stub.createPaymentLink(CreatePaymentLinkRequest.newBuilder()
                    .setEnrollmentId(enrollmentId)
                    .setUserId(userId)
                    .setCurrency(currency)
                    .setAmount(amount)
                    .setPurpose("TREASURE_PLAN")
                    .setPlanId(planId)
                    .build());
        } catch (StatusRuntimeException e) {
            log.error("Create payment link failed: {}", e.getStatus(), e);
            return CreatePaymentLinkResponse.newBuilder().build(); // null object
        }
    }
}
