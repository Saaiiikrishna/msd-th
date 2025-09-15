package com.mysillydreams.treasure.integrations.port;

import java.util.Map;
import java.util.Optional;

public interface NotificationPort {
    Optional<String> send(String userId, String channel, String template, Map<String,String> data);
}
