package com.mysillydreams.treasure.domain.service;

import com.mysillydreams.treasure.domain.repository.ProgressionPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyService {
    private final ProgressionPolicyRepository repo;

    public boolean canUnlockIntermediate(UUID userId, Map<String,Object> counters) {
        // load active policies (global + user), evaluate JSON rules against counters
        return false; // TODO
    }

    public boolean canUnlockAdvanced(UUID userId, Map<String,Object> counters) {
        return false; // TODO
    }
}
