package com.epam.gym.service;

import com.epam.gym.client.TrainerWorkloadClient;
import com.epam.gym.dto.request.TrainerWorkloadRequest;
import com.epam.gym.entity.Trainer;
import com.epam.gym.security.ServiceTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotifyWorkloadService {

    private final TrainerWorkloadClient trainerWorkloadClient;
    private final ServiceTokenProvider serviceTokenProvider; // yangi component

    public void sendNotification(Trainer trainer, LocalDate trainingDate,
                                 Integer duration, TrainerWorkloadRequest.ActionType actionType) {
        try {
            String serviceToken = serviceTokenProvider.getServiceToken();
            String authHeader = "Bearer " + serviceToken;

            String transactionId = getTransactionId();

            TrainerWorkloadRequest workloadRequest = TrainerWorkloadRequest.builder()
                    .trainerUsername(trainer.getUser().getUsername())
                    .trainerFirstName(trainer.getUser().getFirstName())
                    .trainerLastName(trainer.getUser().getLastName())
                    .isActive(trainer.getUser().getIsActive())
                    .trainingDate(trainingDate)
                    .trainingDuration(duration)
                    .actionType(actionType)
                    .build();

            log.info("[TransactionId: {}] Sending workload notification to trainer-workload-service: " +
                            "trainer={}, action={}, date={}, duration={}",
                    transactionId,
                    trainer.getUser().getUsername(),
                    actionType,
                    trainingDate,
                    duration);

            trainerWorkloadClient.processWorkload(workloadRequest, authHeader, transactionId);

            log.info("[TransactionId: {}] Workload notification sent successfully", transactionId);

        } catch (Exception e) {
            log.error("Failed to notify trainer workload service: {}", e.getMessage(), e);
            // Don't fail the main operation - circuit breaker handles this
        }
    }

    // TODO:
    //  Are you using calling user token for internal communication between services?
    //  If so, consider using a special service token for such calls

    private String getTransactionId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String transactionId = attributes.getRequest().getHeader("X-Transaction-Id");
            if (transactionId == null) {
                transactionId = (String) attributes.getRequest().getAttribute("transactionId");
            }
            return transactionId != null ? transactionId : "UNKNOWN";
        }
        return "UNKNOWN";
    }
}
