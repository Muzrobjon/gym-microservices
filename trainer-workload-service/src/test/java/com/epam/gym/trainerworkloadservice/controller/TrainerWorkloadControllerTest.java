package com.epam.gym.trainerworkloadservice.controller;

import com.epam.gym.trainerworkloadservice.dto.TrainerWorkloadRequest;
import com.epam.gym.trainerworkloadservice.dto.TrainerWorkloadResponse;
import com.epam.gym.trainerworkloadservice.enums.ActionType;
import com.epam.gym.trainerworkloadservice.service.TrainerWorkloadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadControllerTest {

    @Mock
    private TrainerWorkloadService workloadService;

    @InjectMocks
    private TrainerWorkloadController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private TrainerWorkloadRequest validRequest;
    private TrainerWorkloadResponse mockResponse;

    @BeforeEach
    void setUp() {
        // ObjectMapper with LocalDate support
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setObjectMapper(objectMapper);

        // Build standalone MockMvc - NO Spring context!
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(jsonConverter)
                .build();

        // Prepare test data
        validRequest = TrainerWorkloadRequest.builder()
                .trainerUsername("john.doe")
                .trainerFirstName("John")
                .trainerLastName("Doe")
                .isActive(true)
                .trainingDate(LocalDate.of(2024, 3, 15))
                .trainingDuration(60)
                .actionType(ActionType.ADD)
                .build();

        TrainerWorkloadResponse.MonthSummary marchSummary = TrainerWorkloadResponse.MonthSummary.builder()
                .month(3)
                .trainingSummaryDuration(120L)
                .build();

        TrainerWorkloadResponse.MonthSummary aprilSummary = TrainerWorkloadResponse.MonthSummary.builder()
                .month(4)
                .trainingSummaryDuration(90L)
                .build();

        TrainerWorkloadResponse.YearSummary yearSummary = TrainerWorkloadResponse.YearSummary.builder()
                .year(2024)
                .months(List.of(marchSummary, aprilSummary))
                .build();

        mockResponse = TrainerWorkloadResponse.builder()
                .trainerUsername("john.doe")
                .trainerFirstName("John")
                .trainerLastName("Doe")
                .trainerStatus(true)
                .years(List.of(yearSummary))
                .build();
    }

    // ==================== POST Tests ====================

    @Test
    @DisplayName("POST - Success with valid request and transaction ID")
    void processWorkload_WithValidRequest_ReturnsOk() throws Exception {
        doNothing().when(workloadService).processWorkload(any(TrainerWorkloadRequest.class), anyString());

        mockMvc.perform(post("/api/trainers/workload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Transaction-Id", "test-tx-001")
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(workloadService, times(1))
                .processWorkload(any(TrainerWorkloadRequest.class), eq("test-tx-001"));
    }

    @Test
    @DisplayName("POST - Success without transaction ID (generates UNKNOWN)")
    void processWorkload_WithoutTransactionId_ReturnsOk() throws Exception {
        doNothing().when(workloadService).processWorkload(any(TrainerWorkloadRequest.class), anyString());

        mockMvc.perform(post("/api/trainers/workload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(workloadService, times(1))
                .processWorkload(any(TrainerWorkloadRequest.class), eq("UNKNOWN"));
    }

    @Test
    @DisplayName("POST - ADD action type")
    void processWorkload_WithAddAction_ReturnsOk() throws Exception {
        validRequest.setActionType(ActionType.ADD);
        doNothing().when(workloadService).processWorkload(any(TrainerWorkloadRequest.class), anyString());

        mockMvc.perform(post("/api/trainers/workload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Transaction-Id", "test-tx-013")
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(workloadService, times(1))
                .processWorkload(any(TrainerWorkloadRequest.class), eq("test-tx-013"));
    }

    @Test
    @DisplayName("POST - DELETE action type")
    void processWorkload_WithDeleteAction_ReturnsOk() throws Exception {
        validRequest.setActionType(ActionType.DELETE);
        doNothing().when(workloadService).processWorkload(any(TrainerWorkloadRequest.class), anyString());

        mockMvc.perform(post("/api/trainers/workload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Transaction-Id", "test-tx-014")
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(workloadService, times(1))
                .processWorkload(any(TrainerWorkloadRequest.class), eq("test-tx-014"));
    }

    @Test
    @DisplayName("POST - Invalid JSON format")
    void processWorkload_WithInvalidJson_ReturnsBadRequest() throws Exception {
        String invalidJson = "{invalid json}";

        mockMvc.perform(post("/api/trainers/workload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Transaction-Id", "test-tx-011")
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(workloadService, never()).processWorkload(any(TrainerWorkloadRequest.class), anyString());
    }

    @Test
    @DisplayName("POST - Empty request body")
    void processWorkload_WithEmptyBody_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/trainers/workload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Transaction-Id", "test-tx-012"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(workloadService, never()).processWorkload(any(TrainerWorkloadRequest.class), anyString());
    }

    @Test
    @DisplayName("POST - Invalid date format")
    void processWorkload_WithInvalidDateFormat_ReturnsBadRequest() throws Exception {
        String invalidJson = """
                {
                    "trainerUsername": "john.doe",
                    "trainerFirstName": "John",
                    "trainerLastName": "Doe",
                    "isActive": true,
                    "trainingDate": "15-03-2024",
                    "trainingDuration": 60,
                    "actionType": "ADD"
                }
                """;

        mockMvc.perform(post("/api/trainers/workload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Transaction-Id", "test-tx-015")
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(workloadService, never()).processWorkload(any(TrainerWorkloadRequest.class), anyString());
    }

    // ==================== GET Tests ====================

    @Test
    @DisplayName("GET - Success: retrieve all workload")
    void getTrainerWorkload_WithoutFilters_ReturnsWorkload() throws Exception {
        when(workloadService.getTrainerWorkload(eq("john.doe"), isNull(), isNull(), anyString()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/trainers/workload/john.doe")
                        .header("X-Transaction-Id", "test-tx-016"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("john.doe"))
                .andExpect(jsonPath("$.trainerFirstName").value("John"))
                .andExpect(jsonPath("$.trainerLastName").value("Doe"))
                .andExpect(jsonPath("$.trainerStatus").value(true))
                .andExpect(jsonPath("$.years", hasSize(1)))
                .andExpect(jsonPath("$.years[0].year").value(2024))
                .andExpect(jsonPath("$.years[0].months", hasSize(2)))
                .andExpect(jsonPath("$.years[0].months[0].month").value(3))
                .andExpect(jsonPath("$.years[0].months[0].trainingSummaryDuration").value(120));

        verify(workloadService, times(1))
                .getTrainerWorkload("john.doe", null, null, "test-tx-016");
    }

    @Test
    @DisplayName("GET - Success: filter by year")
    void getTrainerWorkload_WithYearFilter_ReturnsFilteredWorkload() throws Exception {
        when(workloadService.getTrainerWorkload(eq("john.doe"), eq(2024), isNull(), anyString()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/trainers/workload/john.doe")
                        .param("year", "2024")
                        .header("X-Transaction-Id", "test-tx-017"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("john.doe"))
                .andExpect(jsonPath("$.years[0].year").value(2024));

        verify(workloadService, times(1))
                .getTrainerWorkload("john.doe", 2024, null, "test-tx-017");
    }

    @Test
    @DisplayName("GET - Success: filter by year and month")
    void getTrainerWorkload_WithYearAndMonthFilter_ReturnsFilteredWorkload() throws Exception {
        TrainerWorkloadResponse.MonthSummary marchOnly = TrainerWorkloadResponse.MonthSummary.builder()
                .month(3)
                .trainingSummaryDuration(120L)
                .build();

        TrainerWorkloadResponse.YearSummary filteredYear = TrainerWorkloadResponse.YearSummary.builder()
                .year(2024)
                .months(List.of(marchOnly))
                .build();

        TrainerWorkloadResponse filteredResponse = TrainerWorkloadResponse.builder()
                .trainerUsername("john.doe")
                .trainerFirstName("John")
                .trainerLastName("Doe")
                .trainerStatus(true)
                .years(List.of(filteredYear))
                .build();

        when(workloadService.getTrainerWorkload(eq("john.doe"), eq(2024), eq(3), anyString()))
                .thenReturn(filteredResponse);

        mockMvc.perform(get("/api/trainers/workload/john.doe")
                        .param("year", "2024")
                        .param("month", "3")
                        .header("X-Transaction-Id", "test-tx-018"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.years[0].months", hasSize(1)))
                .andExpect(jsonPath("$.years[0].months[0].month").value(3));

        verify(workloadService, times(1))
                .getTrainerWorkload("john.doe", 2024, 3, "test-tx-018");
    }

    @Test
    @DisplayName("GET - Success: filter by month only")
    void getTrainerWorkload_WithMonthFilterOnly_ReturnsFilteredWorkload() throws Exception {
        when(workloadService.getTrainerWorkload(eq("john.doe"), isNull(), eq(3), anyString()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/trainers/workload/john.doe")
                        .param("month", "3")
                        .header("X-Transaction-Id", "test-tx-019"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("john.doe"));

        verify(workloadService, times(1))
                .getTrainerWorkload("john.doe", null, 3, "test-tx-019");
    }

    @Test
    @DisplayName("GET - Success: without transaction ID")
    void getTrainerWorkload_WithoutTransactionId_ReturnsWorkload() throws Exception {
        when(workloadService.getTrainerWorkload(eq("john.doe"), isNull(), isNull(), anyString()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/trainers/workload/john.doe"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("john.doe"));

        verify(workloadService, times(1))
                .getTrainerWorkload(eq("john.doe"), isNull(), isNull(), eq("UNKNOWN"));
    }

    @Test
    @DisplayName("GET - Empty workload for trainer")
    void getTrainerWorkload_EmptyWorkload_ReturnsEmptyYears() throws Exception {
        TrainerWorkloadResponse emptyResponse = TrainerWorkloadResponse.builder()
                .trainerUsername("jane.smith")
                .trainerFirstName("Jane")
                .trainerLastName("Smith")
                .trainerStatus(true)
                .years(List.of())
                .build();

        when(workloadService.getTrainerWorkload(eq("jane.smith"), isNull(), isNull(), anyString()))
                .thenReturn(emptyResponse);

        mockMvc.perform(get("/api/trainers/workload/jane.smith")
                        .header("X-Transaction-Id", "test-tx-020"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.years", hasSize(0)));

        verify(workloadService, times(1))
                .getTrainerWorkload("jane.smith", null, null, "test-tx-020");
    }

    @Test
    @DisplayName("GET - Trainer not found returns empty response")
    void getTrainerWorkload_TrainerNotFound_ReturnsEmptyResponse() throws Exception {
        TrainerWorkloadResponse notFoundResponse = TrainerWorkloadResponse.builder()
                .trainerUsername("unknown.trainer")
                .years(List.of())
                .build();

        when(workloadService.getTrainerWorkload(eq("unknown.trainer"), isNull(), isNull(), anyString()))
                .thenReturn(notFoundResponse);

        mockMvc.perform(get("/api/trainers/workload/unknown.trainer")
                        .header("X-Transaction-Id", "test-tx-021"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("unknown.trainer"));

        verify(workloadService, times(1))
                .getTrainerWorkload("unknown.trainer", null, null, "test-tx-021");
    }

    @Test
    @DisplayName("GET - Invalid year parameter format")
    void getTrainerWorkload_WithInvalidYearFormat_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/trainers/workload/john.doe")
                        .param("year", "invalid")
                        .header("X-Transaction-Id", "test-tx-022"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(workloadService, never()).getTrainerWorkload(anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("GET - Invalid month parameter format")
    void getTrainerWorkload_WithInvalidMonthFormat_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/trainers/workload/john.doe")
                        .param("month", "invalid")
                        .header("X-Transaction-Id", "test-tx-023"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(workloadService, never()).getTrainerWorkload(anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("GET - Username with special characters")
    void getTrainerWorkload_WithSpecialCharactersInUsername_ReturnsWorkload() throws Exception {
        String specialUsername = "john.doe-123";
        TrainerWorkloadResponse specialResponse = TrainerWorkloadResponse.builder()
                .trainerUsername(specialUsername)
                .years(List.of())
                .build();

        when(workloadService.getTrainerWorkload(eq(specialUsername), isNull(), isNull(), anyString()))
                .thenReturn(specialResponse);

        mockMvc.perform(get("/api/trainers/workload/" + specialUsername)
                        .header("X-Transaction-Id", "test-tx-024"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value(specialUsername));

        verify(workloadService, times(1))
                .getTrainerWorkload(specialUsername, null, null, "test-tx-024");
    }

    @Test
    @DisplayName("GET - Multiple years in response")
    void getTrainerWorkload_WithMultipleYears_ReturnsAllYears() throws Exception {
        TrainerWorkloadResponse.YearSummary year2023 = TrainerWorkloadResponse.YearSummary.builder()
                .year(2023)
                .months(List.of())
                .build();

        TrainerWorkloadResponse.YearSummary year2024 = TrainerWorkloadResponse.YearSummary.builder()
                .year(2024)
                .months(List.of())
                .build();

        TrainerWorkloadResponse multiYearResponse = TrainerWorkloadResponse.builder()
                .trainerUsername("john.doe")
                .trainerFirstName("John")
                .trainerLastName("Doe")
                .trainerStatus(true)
                .years(List.of(year2023, year2024))
                .build();

        when(workloadService.getTrainerWorkload(eq("john.doe"), isNull(), isNull(), anyString()))
                .thenReturn(multiYearResponse);

        mockMvc.perform(get("/api/trainers/workload/john.doe")
                        .header("X-Transaction-Id", "test-tx-025"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.years", hasSize(2)))
                .andExpect(jsonPath("$.years[0].year").value(2023))
                .andExpect(jsonPath("$.years[1].year").value(2024));

        verify(workloadService, times(1))
                .getTrainerWorkload("john.doe", null, null, "test-tx-025");
    }
}