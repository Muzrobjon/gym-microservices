package com.epam.gym.service;

import com.epam.gym.client.TrainerWorkloadClient;
import com.epam.gym.dto.request.TrainerWorkloadRequest;
import com.epam.gym.entity.Trainer;
import com.epam.gym.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotifyWorkloadServiceTest {

    @Mock
    private TrainerWorkloadClient trainerWorkloadClient;

    @InjectMocks
    private NotifyWorkloadService notifyWorkloadService;

    private Trainer trainer;
    private final LocalDate trainingDate = LocalDate.of(2024, 5, 15);

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("john.doe");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setIsActive(true);

        trainer = new Trainer();
        trainer.setUser(user);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void setupRequestContext(String authHeader, String transactionIdHeader, String transactionIdAttr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authHeader != null) {
            request.addHeader("Authorization", authHeader);
        }
        if (transactionIdHeader != null) {
            request.addHeader("X-Transaction-Id", transactionIdHeader);
        }
        if (transactionIdAttr != null) {
            request.setAttribute("transactionId", transactionIdAttr);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("sendNotification: builds request correctly and calls client")
    void sendNotification_success() {
        setupRequestContext("Bearer token123", "tx-abc", null);

        notifyWorkloadService.sendNotification(
                trainer, trainingDate, 60, TrainerWorkloadRequest.ActionType.ADD);

        ArgumentCaptor<TrainerWorkloadRequest> captor = ArgumentCaptor.forClass(TrainerWorkloadRequest.class);
        verify(trainerWorkloadClient).processWorkload(captor.capture(), eq("Bearer token123"), eq("tx-abc"));

        TrainerWorkloadRequest sent = captor.getValue();
        assertThat(sent.getTrainerUsername()).isEqualTo("john.doe");
        assertThat(sent.getTrainerFirstName()).isEqualTo("John");
        assertThat(sent.getTrainerLastName()).isEqualTo("Doe");
        assertThat(sent.getIsActive()).isTrue();
        assertThat(sent.getTrainingDate()).isEqualTo(trainingDate);
        assertThat(sent.getTrainingDuration()).isEqualTo(60);
        assertThat(sent.getActionType()).isEqualTo(TrainerWorkloadRequest.ActionType.ADD);
    }

    @Test
    @DisplayName("sendNotification: uses transactionId from attribute if header missing")
    void sendNotification_usesAttributeWhenHeaderMissing() {
        setupRequestContext("Bearer token123", null, "tx-from-attr");

        notifyWorkloadService.sendNotification(
                trainer, trainingDate, 60, TrainerWorkloadRequest.ActionType.ADD);

        verify(trainerWorkloadClient).processWorkload(any(), eq("Bearer token123"), eq("tx-from-attr"));
    }

    @Test
    @DisplayName("sendNotification: uses UNKNOWN transactionId when no header/attribute")
    void sendNotification_unknownTransactionIdWhenMissing() {
        setupRequestContext("Bearer token123", null, null);

        notifyWorkloadService.sendNotification(
                trainer, trainingDate, 60, TrainerWorkloadRequest.ActionType.ADD);

        verify(trainerWorkloadClient).processWorkload(any(), eq("Bearer token123"), eq("UNKNOWN"));
    }

    @Test
    @DisplayName("sendNotification: uses empty auth header when missing")
    void sendNotification_emptyAuthWhenMissing() {
        setupRequestContext(null, "tx-abc", null);

        notifyWorkloadService.sendNotification(
                trainer, trainingDate, 60, TrainerWorkloadRequest.ActionType.DELETE);

        verify(trainerWorkloadClient).processWorkload(any(), eq(""), eq("tx-abc"));
    }

    @Test
    @DisplayName("sendNotification: uses empty auth and UNKNOWN when request context is absent")
    void sendNotification_noRequestContext() {
        RequestContextHolder.resetRequestAttributes();

        notifyWorkloadService.sendNotification(
                trainer, trainingDate, 45, TrainerWorkloadRequest.ActionType.ADD);

        verify(trainerWorkloadClient).processWorkload(any(), eq(""), eq("UNKNOWN"));
    }

    @Test
    @DisplayName("sendNotification: swallows exception from client (does not fail)")
    void sendNotification_exceptionSwallowed() {
        setupRequestContext("Bearer token123", "tx-abc", null);

        doThrow(new RuntimeException("Client down"))
                .when(trainerWorkloadClient).processWorkload(any(), anyString(), anyString());

        // Should NOT throw
        notifyWorkloadService.sendNotification(
                trainer, trainingDate, 60, TrainerWorkloadRequest.ActionType.ADD);

        verify(trainerWorkloadClient).processWorkload(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("sendNotification: DELETE action is propagated correctly")
    void sendNotification_deleteAction() {
        setupRequestContext("Bearer token123", "tx-del", null);

        notifyWorkloadService.sendNotification(
                trainer, trainingDate, 30, TrainerWorkloadRequest.ActionType.DELETE);

        ArgumentCaptor<TrainerWorkloadRequest> captor = ArgumentCaptor.forClass(TrainerWorkloadRequest.class);
        verify(trainerWorkloadClient).processWorkload(captor.capture(), anyString(), anyString());
        assertThat(captor.getValue().getActionType()).isEqualTo(TrainerWorkloadRequest.ActionType.DELETE);
        assertThat(captor.getValue().getTrainingDuration()).isEqualTo(30);
    }
}