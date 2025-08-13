package com.mysillydreams.treasure.api.rest.controller;

import com.mysillydreams.treasure.api.rest.dto.response.UserLevelSummaryResponse;
import com.mysillydreams.treasure.domain.model.Difficulty;
import com.mysillydreams.treasure.domain.service.UserLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/treasure/v1/users")
@RequiredArgsConstructor
public class UserLevelController {

    private final UserLevelService userLevelService;

    @GetMapping("/{userId}/levels")
    public UserLevelSummaryResponse levels(@PathVariable UUID userId) {
        Map<Difficulty,Integer> m = userLevelService.getSummary(userId);
        return new UserLevelSummaryResponse(
                m.getOrDefault(Difficulty.BEGINNER, 0),
                m.getOrDefault(Difficulty.INTERMEDIATE, 0),
                m.getOrDefault(Difficulty.ADVANCED, 0)
        );
    }

    @PostMapping("/{userId}/progress/recalculate")
    public void recalc(@PathVariable UUID userId) {
        userLevelService.evaluateOnTaskCompletion(userId);
    }
}
