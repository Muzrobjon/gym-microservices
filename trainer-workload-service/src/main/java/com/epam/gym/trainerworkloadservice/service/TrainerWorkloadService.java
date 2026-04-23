package com.epam.gym.trainerworkloadservice.service;




import com.epam.gym.trainerworkloadservice.dto.ActionType;
import com.epam.gym.trainerworkloadservice.dto.TrainerWorkloadRequest;
import com.epam.gym.trainerworkloadservice.dto.TrainerWorkloadResponse;
import com.epam.gym.trainerworkloadservice.entity.TrainerWorkload;
import com.epam.gym.trainerworkloadservice.repository.TrainerWorkloadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainerWorkloadService {

    private final TrainerWorkloadRepository workloadRepository;

    @Transactional
    public void processWorkload(TrainerWorkloadRequest request, String transactionId) {
        log.info("[TransactionId: {}] Processing workload for trainer: {}, action: {}",
                transactionId, request.getTrainerUsername(), request.getActionType());

        int year = request.getTrainingDate().getYear();
        int month = request.getTrainingDate().getMonthValue();

        Optional<TrainerWorkload> existingWorkload = workloadRepository
                .findByTrainerUsernameAndYearAndMonth(
                        request.getTrainerUsername(), year, month);

        if (request.getActionType() == ActionType.ADD) {
            handleAddAction(request, existingWorkload, year, month, transactionId);
        } else if (request.getActionType() == ActionType.DELETE) {
            handleDeleteAction(request, existingWorkload, year, month, transactionId);
        }

        log.info("[TransactionId: {}] Workload processed successfully for trainer: {}",
                transactionId, request.getTrainerUsername());
    }

    private void handleAddAction(TrainerWorkloadRequest request,
                                 Optional<TrainerWorkload> existingWorkload,
                                 int year, int month, String transactionId) {
        if (existingWorkload.isPresent()) {
            TrainerWorkload workload = existingWorkload.get();
            workload.setTrainingSummaryDuration(
                    workload.getTrainingSummaryDuration() + request.getTrainingDuration());
            workload.setTrainerFirstName(request.getTrainerFirstName());
            workload.setTrainerLastName(request.getTrainerLastName());
            workload.setTrainerStatus(request.getIsActive());
            workloadRepository.save(workload);

            log.info("[TransactionId: {}] Updated existing workload for trainer: {}, year: {}, month: {}, " +
                            "new duration: {}",
                    transactionId, request.getTrainerUsername(), year, month,
                    workload.getTrainingSummaryDuration());
        } else {
            TrainerWorkload workload = TrainerWorkload.builder()
                    .trainerUsername(request.getTrainerUsername())
                    .trainerFirstName(request.getTrainerFirstName())
                    .trainerLastName(request.getTrainerLastName())
                    .trainerStatus(request.getIsActive())
                    .year(year)
                    .month(month)
                    .trainingSummaryDuration(Long.valueOf(request.getTrainingDuration()))
                    .build();
            workloadRepository.save(workload);

            log.info("[TransactionId: {}] Created new workload for trainer: {}, year: {}, month: {}, " +
                            "duration: {}",
                    transactionId, request.getTrainerUsername(), year, month,
                    request.getTrainingDuration());
        }
    }

    private void handleDeleteAction(TrainerWorkloadRequest request,
                                    Optional<TrainerWorkload> existingWorkload,
                                    int year, int month, String transactionId) {
        if (existingWorkload.isPresent()) {
            TrainerWorkload workload = existingWorkload.get();
            long newDuration = workload.getTrainingSummaryDuration() - request.getTrainingDuration();

            if (newDuration <= 0) {
                workloadRepository.delete(workload);
                log.info("[TransactionId: {}] Deleted workload record for trainer: {}, year: {}, month: {}",
                        transactionId, request.getTrainerUsername(), year, month);
            } else {
                workload.setTrainingSummaryDuration(newDuration);
                workloadRepository.save(workload);
                log.info("[TransactionId: {}] Reduced workload for trainer: {}, year: {}, month: {}, " +
                                "new duration: {}",
                        transactionId, request.getTrainerUsername(), year, month, newDuration);
            }
        } else {
            log.warn("[TransactionId: {}] No workload found for trainer: {}, year: {}, month: {}. " +
                            "Cannot delete.",
                    transactionId, request.getTrainerUsername(), year, month);
        }
    }

    @Transactional(readOnly = true)
    public TrainerWorkloadResponse getTrainerWorkload(String trainerUsername, String transactionId) {
        log.info("[TransactionId: {}] Fetching workload for trainer: {}", transactionId, trainerUsername);

        List<TrainerWorkload> workloads = workloadRepository
                .findByTrainerUsernameOrderByYearAscMonthAsc(trainerUsername);

        if (workloads.isEmpty()) {
            log.info("[TransactionId: {}] No workload found for trainer: {}", transactionId, trainerUsername);
            return TrainerWorkloadResponse.builder()
                    .trainerUsername(trainerUsername)
                    .years(Collections.emptyList())
                    .build();
        }

        // Get trainer info from first record
        TrainerWorkload first = workloads.get(0);

        // Group by year, then by month
        Map<Integer, List<TrainerWorkload>> byYear = workloads.stream()
                .collect(Collectors.groupingBy(TrainerWorkload::getYear, TreeMap::new, Collectors.toList()));

        List<TrainerWorkloadResponse.YearSummary> yearSummaries = byYear.entrySet().stream()
                .map(yearEntry -> {
                    List<TrainerWorkloadResponse.MonthSummary> monthSummaries = yearEntry.getValue().stream()
                            .map(w -> TrainerWorkloadResponse.MonthSummary.builder()
                                    .month(w.getMonth())
                                    .trainingSummaryDuration(w.getTrainingSummaryDuration())
                                    .build())
                            .sorted(Comparator.comparing(TrainerWorkloadResponse.MonthSummary::getMonth))
                            .collect(Collectors.toList());

                    return TrainerWorkloadResponse.YearSummary.builder()
                            .year(yearEntry.getKey())
                            .months(monthSummaries)
                            .build();
                })
                .collect(Collectors.toList());

        TrainerWorkloadResponse response = TrainerWorkloadResponse.builder()
                .trainerUsername(first.getTrainerUsername())
                .trainerFirstName(first.getTrainerFirstName())
                .trainerLastName(first.getTrainerLastName())
                .trainerStatus(first.getTrainerStatus())
                .years(yearSummaries)
                .build();

        log.info("[TransactionId: {}] Workload retrieved for trainer: {} with {} year entries",
                transactionId, trainerUsername, yearSummaries.size());

        return response;
    }
}