package com.mysillydreams.treasure.integrations.adapter;

import com.mysillydreams.treasure.integrations.port.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(prefix="feature.integrations.notifications", name="enabled", havingValue="false", matchIfMissing = true)
public class NotificationNoopAdapter implements NotificationPort {
    @Override
    public Optional<String> send(String userId, String channel, String template, Map<String, String> data) {
        log.info("[NOOP] Notification send userId={} channel={} template={} data={}", userId, channel, template, data);
        return Optional.empty();
    }
}
