package com.epam.gym.service;

import com.epam.gym.dto.request.TraineeRegistrationRequest;
import com.epam.gym.dto.request.TrainerWorkloadRequest;
import com.epam.gym.dto.request.UpdateTraineeRequest;
import com.epam.gym.dto.response.RegistrationResponse;
import com.epam.gym.entity.Trainee;
import com.epam.gym.entity.Trainer;
import com.epam.gym.entity.Training;
import com.epam.gym.entity.User;
import com.epam.gym.exception.NotFoundException;
import com.epam.gym.exception.ValidationException;
import com.epam.gym.repository.TraineeRepository;
import com.epam.gym.repository.TrainerRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraineeServiceTest {

    @Mock private NotifyWorkloadService notifyWorkloadService;
    @Mock private PasswordService passwordService;
    @Mock private TraineeRepository traineeRepository;
    @Mock private TrainerRepository trainerRepository;
    @Mock private UserService userService;
    @Mock private Validator validator;

    @InjectMocks
    private TraineeService traineeService;

    private User user;
    private Trainee trainee;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("jane.doe");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setPassword("rawPassword");
        user.setIsActive(true);

        trainee = Trainee.builder()
                .id(1L)
                .user(user)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .address("123 Street")
                .trainers(new ArrayList<>())
                .trainings(new ArrayList<>())
                .build();
    }

    // ============ createProfile ============

    @Test
    @DisplayName("createProfile: creates trainee and returns raw password")
    void createProfile_success() {
        TraineeRegistrationRequest request = new TraineeRegistrationRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setDateOfBirth(LocalDate.of(2000, 1, 1));
        request.setAddress("123 Street");

        when(userService.createUser("Jane", "Doe")).thenReturn(user);
        when(passwordService.encodePassword("rawPassword")).thenReturn("encodedPassword");
        when(validator.validate(any(Trainee.class))).thenReturn(Set.of());
        when(traineeRepository.save(any(Trainee.class))).thenReturn(trainee);

        RegistrationResponse response = traineeService.createProfile(request);

        assertThat(response.getUsername()).isEqualTo("jane.doe");
        assertThat(response.getPassword()).isEqualTo("rawPassword");

        ArgumentCaptor<Trainee> captor = ArgumentCaptor.forClass(Trainee.class);
        verify(traineeRepository).save(captor.capture());
        Trainee saved = captor.getValue();
        assertThat(saved.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(saved.getAddress()).isEqualTo("123 Street");
        assertThat(saved.getUser().getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    @DisplayName("createProfile: throws ValidationException when invalid")
    @SuppressWarnings("unchecked")
    void createProfile_validationFails() {
        TraineeRegistrationRequest request = new TraineeRegistrationRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");

        when(userService.createUser(anyString(), anyString())).thenReturn(user);
        when(passwordService.encodePassword(anyString())).thenReturn("encoded");

        ConstraintViolation<Trainee> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Address required");
        when(validator.validate(any(Trainee.class))).thenReturn(Set.of(violation));

        assertThatThrownBy(() -> traineeService.createProfile(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Address required");

        verify(traineeRepository, never()).save(any());
    }

    // ============ getByUsername ============

    @Test
    @DisplayName("getByUsername: returns trainee if found")
    void getByUsername_found() {
        when(traineeRepository.findByUser_Username("jane.doe")).thenReturn(Optional.of(trainee));

        Trainee result = traineeService.getByUsername("jane.doe");

        assertThat(result).isEqualTo(trainee);
    }

    @Test
    @DisplayName("getByUsername: throws NotFoundException when missing")
    void getByUsername_notFound() {
        when(traineeRepository.findByUser_Username("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> traineeService.getByUsername("unknown"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Trainee not found: unknown");
    }

    // ============ updateProfile ============

    @Test
    @DisplayName("updateProfile: updates fields and saves")
    void updateProfile_success() {
        UpdateTraineeRequest request = new UpdateTraineeRequest();
        request.setUsername("jane.doe");
        request.setFirstName("JaneUpdated");
        request.setLastName("DoeUpdated");
        request.setIsActive(false);
        request.setDateOfBirth(LocalDate.of(1999, 12, 31));
        request.setAddress("New Address");

        when(traineeRepository.findByUser_Username("jane.doe")).thenReturn(Optional.of(trainee));
        when(validator.validate(any(Trainee.class))).thenReturn(Set.of());
        when(traineeRepository.save(trainee)).thenReturn(trainee);

        Trainee result = traineeService.updateProfile("jane.doe", request);

        verify(userService).isAuthenticated("jane.doe");
        verify(userService).updateUserBasicInfo(user, "JaneUpdated", "DoeUpdated", false);
        assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1999, 12, 31));
        assertThat(result.getAddress()).isEqualTo("New Address");
    }

    @Test
    @DisplayName("updateProfile: keeps old dateOfBirth/address if null in request")
    void updateProfile_partialUpdate() {
        UpdateTraineeRequest request = new UpdateTraineeRequest();
        request.setUsername("jane.doe");
        request.setFirstName("JaneUpdated");
        request.setLastName("DoeUpdated");
        request.setIsActive(true);
        request.setDateOfBirth(null);
        request.setAddress(null);

        when(traineeRepository.findByUser_Username("jane.doe")).thenReturn(Optional.of(trainee));
        when(validator.validate(any(Trainee.class))).thenReturn(Set.of());
        when(traineeRepository.save(trainee)).thenReturn(trainee);

        traineeService.updateProfile("jane.doe", request);

        // Unchanged
        assertThat(trainee.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(trainee.getAddress()).isEqualTo("123 Street");
    }

    @Test
    @DisplayName("updateProfile: throws NotFoundException if trainee missing")
    void updateProfile_traineeNotFound() {
        UpdateTraineeRequest request = new UpdateTraineeRequest();
        request.setUsername("jane.doe");

        when(traineeRepository.findByUser_Username("jane.doe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> traineeService.updateProfile("jane.doe", request))
                .isInstanceOf(NotFoundException.class);
    }

    // ============ deleteByUsername ============

    @Test
    @DisplayName("deleteByUsername: deletes trainee and notifies workload for each training")
    void deleteByUsername_withTrainings() {
        Trainer trainer = new Trainer();
        User trainerUser = new User();
        trainerUser.setUsername("john.trainer");
        trainer.setUser(trainerUser);

        Training t1 = Training.builder()
                .trainer(trainer)
                .trainingDate(LocalDate.of(2024, 5, 15))
                .trainingDurationMinutes(60)
                .build();
        Training t2 = Training.builder()
                .trainer(trainer)
                .trainingDate(LocalDate.of(2024, 6, 10))
                .trainingDurationMinutes(45)
                .build();

        trainee.setTrainings(new ArrayList<>(List.of(t1, t2)));

        when(traineeRepository.findByUser_Username("jane.doe")).thenReturn(Optional.of(trainee));

        traineeService.deleteByUsername("jane.doe");

        verify(userService).isAuthenticated("jane.doe");
        verify(notifyWorkloadService).sendNotification(
                trainer, LocalDate.of(2024, 5, 15), 60, TrainerWorkloadRequest.ActionType.DELETE);
        verify(notifyWorkloadService).sendNotification(
                trainer, LocalDate.of(2024, 6, 10), 45, TrainerWorkloadRequest.ActionType.DELETE);
        verify(traineeRepository).delete(trainee);
    }

    @Test
    @DisplayName("deleteByUsername: deletes even with no trainings")
    void deleteByUsername_noTrainings() {
        trainee.setTrainings(new ArrayList<>());
        when(traineeRepository.findByUser_Username("jane.doe")).thenReturn(Optional.of(trainee));

        traineeService.deleteByUsername("jane.doe");

        verify(notifyWorkloadService, never()).sendNotification(any(), any(), any(), any());
        verify(traineeRepository).delete(trainee);
    }

    @Test
    @DisplayName("deleteByUsername: throws NotFoundException if trainee missing")
    void deleteByUsername_notFound() {
        when(traineeRepository.findByUser_Username("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> traineeService.deleteByUsername("unknown"))
                .isInstanceOf(NotFoundException.class);

        verify(traineeRepository, never()).delete(any());
    }

    // ============ updateTrainersList ============

    @Test
    @DisplayName("updateTrainersList: replaces trainers list")
    void updateTrainersList_success() {
        Trainer trainer1 = buildTrainer("t1");
        Trainer trainer2 = buildTrainer("t2");

        when(traineeRepository.findByUser_Username("jane.doe")).thenReturn(Optional.of(trainee));
        when(trainerRepository.findByUser_UsernameIn(List.of("t1", "t2")))
                .thenReturn(List.of(trainer1, trainer2));

        List<Trainer> result = traineeService.updateTrainersList("jane.doe", List.of("t1", "t2"));

        verify(userService).isAuthenticated("jane.doe");
        assertThat(result).hasSize(2);
        assertThat(trainee.getTrainers()).containsExactly(trainer1, trainer2);
        verify(traineeRepository).save(trainee);
    }

    @Test
    @DisplayName("updateTrainersList: empty list clears trainers")
    void updateTrainersList_emptyList() {
        trainee.getTrainers().add(buildTrainer("old"));

        when(traineeRepository.findByUser_Username("jane.doe")).thenReturn(Optional.of(trainee));

        List<Trainer> result = traineeService.updateTrainersList("jane.doe", List.of());

        assertThat(result).isEmpty();
        assertThat(trainee.getTrainers()).isEmpty();
        verify(trainerRepository, never()).findByUser_UsernameIn(any());
    }

    @Test
    @DisplayName("updateTrainersList: null list treated as empty")
    void updateTrainersList_nullList() {
        when(traineeRepository.findByUser_Username("jane.doe")).thenReturn(Optional.of(trainee));

        List<Trainer> result = traineeService.updateTrainersList("jane.doe", null);

        assertThat(result).isEmpty();
        verify(trainerRepository, never()).findByUser_UsernameIn(any());
    }

    @Test
    @DisplayName("updateTrainersList: throws NotFoundException for missing trainers")
    void updateTrainersList_someTrainersMissing() {
        Trainer t1 = buildTrainer("t1");

        when(traineeRepository.findByUser_Username("jane.doe")).thenReturn(Optional.of(trainee));
        when(trainerRepository.findByUser_UsernameIn(List.of("t1", "missing")))
                .thenReturn(List.of(t1));

        assertThatThrownBy(() -> traineeService.updateTrainersList("jane.doe", List.of("t1", "missing")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("missing");
    }

    // ============ helpers ============

    private Trainer buildTrainer(String username) {
        User u = new User();
        u.setUsername(username);
        Trainer t = new Trainer();
        t.setUser(u);
        return t;
    }
}