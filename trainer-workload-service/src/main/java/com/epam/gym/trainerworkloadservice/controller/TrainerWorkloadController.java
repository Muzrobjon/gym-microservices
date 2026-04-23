package com.epam.gym.trainerworkloadservice.controller;

import com.epam.gym.trainerworkloadservice.dto.TrainerWorkloadRequest;
import com.epam.gym.trainerworkloadservice.dto.TrainerWorkloadResponse;
import com.epam.gym.trainerworkloadservice.service.TrainerWorkloadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/trainers/workload")
@RequiredArgsConstructor
public class TrainerWorkloadController {

    private final TrainerWorkloadService workloadService;

    @PostMapping
    public ResponseEntity<Void> processWorkload(
            @Valid @RequestBody TrainerWorkloadRequest request,
            HttpServletRequest httpRequest) {

        String transactionId = getTransactionId(httpRequest);

        log.info("[TransactionId: {}] Received workload request - Trainer: {}, Action: {}, Date: {}, Duration: {}",
                transactionId,
                request.getTrainerUsername(),
                request.getActionType(),
                request.getTrainingDate(),
                request.getTrainingDuration());

        workloadService.processWorkload(request, transactionId);

        log.info("[TransactionId: {}] Workload processed successfully - Status: 200 OK", transactionId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{trainerUsername}")
    public ResponseEntity<TrainerWorkloadResponse> getTrainerWorkload(
            @PathVariable String trainerUsername,
            HttpServletRequest httpRequest) {

        String transactionId = getTransactionId(httpRequest);

        log.info("[TransactionId: {}] Received get workload request for trainer: {}",
                transactionId, trainerUsername);

        TrainerWorkloadResponse response = workloadService.getTrainerWorkload(trainerUsername, transactionId);

        log.info("[TransactionId: {}] Returning workload for trainer: {} - Status: 200 OK",
                transactionId, trainerUsername);

        return ResponseEntity.ok(response);
    }

    private String getTransactionId(HttpServletRequest request) {
        String transactionId = request.getHeader("X-Transaction-Id");
        if (transactionId == null || transactionId.isEmpty()) {
            transactionId = (String) request.getAttribute("transactionId");
        }
        return transactionId != null ? transactionId : "UNKNOWN";
    }
}