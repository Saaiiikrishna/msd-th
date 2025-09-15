package com.mysillydreams.treasure.domain.service;

import com.mysillydreams.treasure.domain.model.*;
import com.mysillydreams.treasure.domain.repository.EnrollmentRepository;
import com.mysillydreams.treasure.domain.repository.TaskProgressRepository;
import com.mysillydreams.treasure.messaging.producer.EnrollmentEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskProgressService {
    private final TaskProgressRepository progressRepo;
    private final EnrollmentRepository enrollRepo;
    private final UserLevelService userLevelService;
    private final EnrollmentEventProducer eventProducer;

    @Transactional
    public TaskProgress completeTask(UUID enrollmentId, UUID taskId) {
        Enrollment e = enrollRepo.findById(enrollmentId).orElseThrow();
        TaskProgress p = progressRepo.findByEnrollmentIdAndTaskId(enrollmentId, taskId)
                .orElse(TaskProgress.builder().enrollment(e).task(Task.builder().id(taskId).build()).status(TaskStatus.STARTED).build());
        p.setStatus(TaskStatus.DONE);
        TaskProgress saved = progressRepo.save(p);

        eventProducer.taskCompleted(e, taskId);
        userLevelService.evaluateOnTaskCompletion(e.getUserId());
        return saved;
    }
}

