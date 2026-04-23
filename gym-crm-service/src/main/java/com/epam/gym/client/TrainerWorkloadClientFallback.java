package com.epam.gym.client;

import com.epam.gym.dto.request.TrainerWorkloadRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TrainerWorkloadClientFallback implements TrainerWorkloadClient {

    @Override
    public ResponseEntity<Void> processWorkload(
            TrainerWorkloadRequest request,
            String authorizationHeader,
            String transactionId) {

        log.error("[TransactionId: {}] Circuit breaker activated - " +
                        "Failed to send workload to trainer-workload-service for trainer: {}. " +
                        "Action: {}, Date: {}, Duration: {}",
                transactionId,
                request.getTrainerUsername(),
                request.getActionType(),
                request.getTrainingDate(),
                request.getTrainingDuration());

        return ResponseEntity.ok().build();
    }
}