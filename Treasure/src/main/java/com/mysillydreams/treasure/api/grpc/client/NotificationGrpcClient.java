package com.mysillydreams.treasure.api.grpc.client;

import com.mysillydreams.notifications.v1.NotificationServiceGrpc;
import com.mysillydreams.notifications.v1.SendRequest;
import com.mysillydreams.notifications.v1.SendResponse;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationGrpcClient {

    @GrpcClient("notifications")
    private NotificationServiceGrpc.NotificationServiceBlockingStub stub;

    public String send(String userId, String channel, String template, Map<String,String> data) {
        try {
            SendResponse rsp = stub.send(SendRequest.newBuilder()
                    .setUserId(userId).setChannel(channel).setTemplate(template)
                    .putAllData(data).build());
            return rsp.getMessageId();
        } catch (StatusRuntimeException e) {
            log.warn("Notification send failed: {}", e.getStatus(), e);
            return null;
        }
    }
}
