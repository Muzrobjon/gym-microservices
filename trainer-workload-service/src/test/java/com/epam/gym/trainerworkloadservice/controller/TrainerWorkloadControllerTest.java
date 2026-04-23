package com.epam.gym.trainerworkloadservice.controller;

import com.epam.gym.trainerworkloadservice.dto.ActionType;
import com.epam.gym.trainerworkloadservice.dto.TrainerWorkloadRequest;
import com.epam.gym.trainerworkloadservice.dto.TrainerWorkloadResponse;
import com.epam.gym.trainerworkloadservice.security.JwtAuthenticationFilter;
import com.epam.gym.trainerworkloadservice.service.TrainerWorkloadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TrainerWorkloadController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class)
)
class TrainerWorkloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrainerWorkloadService workloadService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @WithMockUser
    void processWorkload_validRequest_returns200() throws Exception {
        TrainerWorkloadRequest request = TrainerWorkloadRequest.builder()
                .trainerUsername("john.doe")
                .trainerFirstName("John")
                .trainerLastName("Doe")
                .isActive(true)
                .trainingDate(LocalDate.of(2024, 5, 15))
                .trainingDuration(60)
                .actionType(ActionType.ADD)
                .build();

        mockMvc.perform(post("/api/trainers/workload")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(workloadService).processWorkload(any(TrainerWorkloadRequest.class), anyString());
    }

    @Test
    @WithMockUser
    void processWorkload_invalidRequest_returns400() throws Exception {
        TrainerWorkloadRequest invalidRequest = TrainerWorkloadRequest.builder()
                .trainerUsername("")  // invalid
                .trainingDuration(-5) // invalid
                .build();

        mockMvc.perform(post("/api/trainers/workload")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(workloadService, never()).processWorkload(any(), anyString());
    }

    @Test
    @WithMockUser
    void getTrainerWorkload_returnsResponse() throws Exception {
        TrainerWorkloadResponse response = TrainerWorkloadResponse.builder()
                .trainerUsername("john.doe")
                .trainerFirstName("John")
                .trainerLastName("Doe")
                .trainerStatus(true)
                .years(List.of())
                .build();

        when(workloadService.getTrainerWorkload(eq("john.doe"), anyString())).thenReturn(response);

        mockMvc.perform(get("/api/trainers/workload/john.doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("john.doe"))
                .andExpect(jsonPath("$.trainerFirstName").value("John"));
    }

    @Test
    @WithMockUser
    void processWorkload_withTransactionIdHeader_usesProvidedId() throws Exception {
        TrainerWorkloadRequest request = TrainerWorkloadRequest.builder()
                .trainerUsername("john.doe").trainerFirstName("John").trainerLastName("Doe")
                .isActive(true).trainingDate(LocalDate.of(2024, 5, 15))
                .trainingDuration(60).actionType(ActionType.ADD).build();

        mockMvc.perform(post("/api/trainers/workload")
                        .with(csrf())
                        .header("X-Transaction-Id", "custom-tx-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(workloadService).processWorkload(any(), eq("custom-tx-id"));
    }
}