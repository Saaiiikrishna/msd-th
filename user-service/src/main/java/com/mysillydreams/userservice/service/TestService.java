package com.mysillydreams.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
@Slf4j
public class TestService {
    
    @PostConstruct
    public void init() {
        log.info("🧪 TestService has been instantiated and is ready");
    }
}
