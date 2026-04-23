package com.epam.gym.trainerworkloadservice.service;

import com.epam.gym.trainerworkloadservice.dto.ActionType;
import com.epam.gym.trainerworkloadservice.dto.TrainerWorkloadRequest;
import com.epam.gym.trainerworkloadservice.dto.TrainerWorkloadResponse;
import com.epam.gym.trainerworkloadservice.entity.TrainerWorkload;
import com.epam.gym.trainerworkloadservice.repository.TrainerWorkloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadServiceTest {

    @Mock
    private TrainerWorkloadRepository workloadRepository;

    @InjectMocks
    private TrainerWorkloadService workloadService;

    private TrainerWorkloadRequest addRequest;
    private TrainerWorkloadRequest deleteRequest;
    private final String transactionId = "test-tx-id";

    @BeforeEach
    void setUp() {
        addRequest = TrainerWorkloadRequest.builder()
                .trainerUsername("john.doe")
                .trainerFirstName("John")
                .trainerLastName("Doe")
                .isActive(true)
                .trainingDate(LocalDate.of(2024, 5, 15))
                .trainingDuration(60)
                .actionType(ActionType.ADD)
                .build();

        deleteRequest = TrainerWorkloadRequest.builder()
                .trainerUsername("john.doe")
                .trainerFirstName("John")
                .trainerLastName("Doe")
                .isActive(true)
                .trainingDate(LocalDate.of(2024, 5, 15))
                .trainingDuration(30)
                .actionType(ActionType.DELETE)
                .build();
    }

    @Test
    @DisplayName("ADD action: creates new workload when none exists")
    void processWorkload_addAction_createsNewWorkload() {
        when(workloadRepository.findByTrainerUsernameAndYearAndMonth("john.doe", 2024, 5))
                .thenReturn(Optional.empty());

        workloadService.processWorkload(addRequest, transactionId);

        ArgumentCaptor<TrainerWorkload> captor = ArgumentCaptor.forClass(TrainerWorkload.class);
        verify(workloadRepository).save(captor.capture());

        TrainerWorkload saved = captor.getValue();
        assertThat(saved.getTrainerUsername()).isEqualTo("john.doe");
        assertThat(saved.getYear()).isEqualTo(2024);
        assertThat(saved.getMonth()).isEqualTo(5);
        assertThat(saved.getTrainingSummaryDuration()).isEqualTo(60L);
    }

    @Test
    @DisplayName("ADD action: updates existing workload")
    void processWorkload_addAction_updatesExistingWorkload() {
        TrainerWorkload existing = TrainerWorkload.builder()
                .id(1L)
                .trainerUsername("john.doe")
                .trainerFirstName("John")
                .trainerLastName("Doe")
                .trainerStatus(true)
                .year(2024).month(5)
                .trainingSummaryDuration(100L)
                .build();

        when(workloadRepository.findByTrainerUsernameAndYearAndMonth("john.doe", 2024, 5))
                .thenReturn(Optional.of(existing));

        workloadService.processWorkload(addRequest, transactionId);

        ArgumentCaptor<TrainerWorkload> captor = ArgumentCaptor.forClass(TrainerWorkload.class);
        verify(workloadRepository).save(captor.capture());
        assertThat(captor.getValue().getTrainingSummaryDuration()).isEqualTo(160L);
    }

    @Test
    @DisplayName("DELETE action: reduces duration when result > 0")
    void processWorkload_deleteAction_reducesDuration() {
        TrainerWorkload existing = TrainerWorkload.builder()
                .id(1L)
                .trainerUsername("john.doe")
                .year(2024).month(5)
                .trainingSummaryDuration(100L)
                .build();

        when(workloadRepository.findByTrainerUsernameAndYearAndMonth("john.doe", 2024, 5))
                .thenReturn(Optional.of(existing));

        workloadService.processWorkload(deleteRequest, transactionId);

        ArgumentCaptor<TrainerWorkload> captor = ArgumentCaptor.forClass(TrainerWorkload.class);
        verify(workloadRepository).save(captor.capture());
        assertThat(captor.getValue().getTrainingSummaryDuration()).isEqualTo(70L);
        verify(workloadRepository, never()).delete(any());
    }

    @Test
    @DisplayName("DELETE action: deletes record when newDuration <= 0")
    void processWorkload_deleteAction_deletesRecord() {
        TrainerWorkload existing = TrainerWorkload.builder()
                .id(1L)
                .trainerUsername("john.doe")
                .year(2024).month(5)
                .trainingSummaryDuration(20L)
                .build();

        when(workloadRepository.findByTrainerUsernameAndYearAndMonth("john.doe", 2024, 5))
                .thenReturn(Optional.of(existing));

        workloadService.processWorkload(deleteRequest, transactionId);

        verify(workloadRepository).delete(existing);
        verify(workloadRepository, never()).save(any());
    }

    @Test
    @DisplayName("DELETE action: does nothing when record not found")
    void processWorkload_deleteAction_noRecordFound() {
        when(workloadRepository.findByTrainerUsernameAndYearAndMonth("john.doe", 2024, 5))
                .thenReturn(Optional.empty());

        workloadService.processWorkload(deleteRequest, transactionId);

        verify(workloadRepository, never()).save(any());
        verify(workloadRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getTrainerWorkload: returns empty response when no records")
    void getTrainerWorkload_noRecords_returnsEmptyResponse() {
        when(workloadRepository.findByTrainerUsernameOrderByYearAscMonthAsc("john.doe"))
                .thenReturn(List.of());

        TrainerWorkloadResponse response = workloadService.getTrainerWorkload("john.doe", transactionId);

        assertThat(response.getTrainerUsername()).isEqualTo("john.doe");
        assertThat(response.getYears()).isEmpty();
    }

    @Test
    @DisplayName("getTrainerWorkload: groups records by year and month")
    void getTrainerWorkload_groupsByYearAndMonth() {
        TrainerWorkload w1 = TrainerWorkload.builder()
                .trainerUsername("john.doe").trainerFirstName("John").trainerLastName("Doe")
                .trainerStatus(true).year(2024).month(5).trainingSummaryDuration(60L).build();
        TrainerWorkload w2 = TrainerWorkload.builder()
                .trainerUsername("john.doe").trainerFirstName("John").trainerLastName("Doe")
                .trainerStatus(true).year(2024).month(6).trainingSummaryDuration(90L).build();
        TrainerWorkload w3 = TrainerWorkload.builder()
                .trainerUsername("john.doe").trainerFirstName("John").trainerLastName("Doe")
                .trainerStatus(true).year(2025).month(1).trainingSummaryDuration(30L).build();

        when(workloadRepository.findByTrainerUsernameOrderByYearAscMonthAsc("john.doe"))
                .thenReturn(List.of(w1, w2, w3));

        TrainerWorkloadResponse response = workloadService.getTrainerWorkload("john.doe", transactionId);

        assertThat(response.getTrainerUsername()).isEqualTo("john.doe");
        assertThat(response.getTrainerFirstName()).isEqualTo("John");
        assertThat(response.getYears()).hasSize(2);
        assertThat(response.getYears().get(0).getYear()).isEqualTo(2024);
        assertThat(response.getYears().get(0).getMonths()).hasSize(2);
        assertThat(response.getYears().get(1).getYear()).isEqualTo(2025);
        assertThat(response.getYears().get(1).getMonths()).hasSize(1);
    }
}