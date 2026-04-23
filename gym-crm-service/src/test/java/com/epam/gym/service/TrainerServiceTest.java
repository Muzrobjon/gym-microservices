package com.epam.gym.service;

import com.epam.gym.dto.request.TrainerRegistrationRequest;
import com.epam.gym.dto.request.UpdateTrainerRequest;
import com.epam.gym.dto.response.RegistrationResponse;
import com.epam.gym.entity.Trainee;
import com.epam.gym.entity.Trainer;
import com.epam.gym.entity.TrainingType;
import com.epam.gym.entity.User;
import com.epam.gym.enums.TrainingTypeName;
import com.epam.gym.exception.NotFoundException;
import com.epam.gym.exception.ValidationException;
import com.epam.gym.repository.TraineeRepository;
import com.epam.gym.repository.TrainerRepository;
import com.epam.gym.repository.TrainingTypeRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerServiceTest {

    @Mock private PasswordService passwordService;
    @Mock private TrainerRepository trainerRepository;
    @Mock private TrainingTypeRepository trainingTypeRepository;
    @Mock private TraineeRepository traineeRepository;
    @Mock private UserService userService;
    @Mock private Validator validator;

    @InjectMocks
    private TrainerService trainerService;

    private User user;
    private TrainingType trainingType;
    private Trainer trainer;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("john.trainer");
        user.setFirstName("John");
        user.setLastName("Trainer");
        user.setPassword("rawPassword");
        user.setIsActive(true);

        trainingType = new TrainingType();
        trainingType.setTrainingTypeName(TrainingTypeName.FITNESS);

        trainer = Trainer.builder()
                .id(1L)
                .user(user)
                .specialization(trainingType)
                .trainees(List.of())
                .build();
    }

    // ============ createProfile ============

    @Test
    @DisplayName("createProfile: success")
    void createProfile_success() {
        TrainerRegistrationRequest request = new TrainerRegistrationRequest();
        request.setFirstName("John");
        request.setLastName("Trainer");
        request.setSpecialization(TrainingTypeName.FITNESS);

        when(userService.createUser("John", "Trainer")).thenReturn(user);
        when(passwordService.encodePassword("rawPassword")).thenReturn("encodedPassword");
        when(trainingTypeRepository.findByTrainingTypeName(TrainingTypeName.FITNESS))
                .thenReturn(Optional.of(trainingType));
        when(validator.validate(any(Trainer.class))).thenReturn(Set.of());
        when(trainerRepository.save(any(Trainer.class))).thenReturn(trainer);

        RegistrationResponse response = trainerService.createProfile(request);

        assertThat(response.getUsername()).isEqualTo("john.trainer");
        assertThat(response.getPassword()).isEqualTo("rawPassword");

        ArgumentCaptor<Trainer> captor = ArgumentCaptor.forClass(Trainer.class);
        verify(trainerRepository).save(captor.capture());
        Trainer saved = captor.getValue();
        assertThat(saved.getSpecialization()).isEqualTo(trainingType);
        assertThat(saved.getUser().getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    @DisplayName("createProfile: throws NotFoundException when training type missing")
    void createProfile_trainingTypeNotFound() {
        TrainerRegistrationRequest request = new TrainerRegistrationRequest();
        request.setFirstName("John");
        request.setLastName("Trainer");
        request.setSpecialization(TrainingTypeName.FITNESS);

        when(userService.createUser(anyString(), anyString())).thenReturn(user);
        when(passwordService.encodePassword(anyString())).thenReturn("encoded");
        when(trainingTypeRepository.findByTrainingTypeName(TrainingTypeName.FITNESS))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainerService.createProfile(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Training type not found");

        verify(trainerRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProfile: throws ValidationException when invalid")
    @SuppressWarnings("unchecked")
    void createProfile_validationFails() {
        TrainerRegistrationRequest request = new TrainerRegistrationRequest();
        request.setFirstName("John");
        request.setLastName("Trainer");
        request.setSpecialization(TrainingTypeName.FITNESS);

        when(userService.createUser(anyString(), anyString())).thenReturn(user);
        when(passwordService.encodePassword(anyString())).thenReturn("encoded");
        when(trainingTypeRepository.findByTrainingTypeName(any()))
                .thenReturn(Optional.of(trainingType));

        ConstraintViolation<Trainer> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Invalid trainer");
        when(validator.validate(any(Trainer.class))).thenReturn(Set.of(violation));

        assertThatThrownBy(() -> trainerService.createProfile(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid trainer");

        verify(trainerRepository, never()).save(any());
    }

    // ============ getByUsername ============

    @Test
    @DisplayName("getByUsername: returns trainer if found")
    void getByUsername_found() {
        when(trainerRepository.findByUser_Username("john.trainer")).thenReturn(Optional.of(trainer));

        Trainer result = trainerService.getByUsername("john.trainer");

        assertThat(result).isEqualTo(trainer);
    }

    @Test
    @DisplayName("getByUsername: throws NotFoundException if not found")
    void getByUsername_notFound() {
        when(trainerRepository.findByUser_Username("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainerService.getByUsername("unknown"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Trainer not found: unknown");
    }

    // ============ updateProfile ============

    @Test
    @DisplayName("updateProfile: updates fields and saves")
    void updateProfile_success() {
        UpdateTrainerRequest request = new UpdateTrainerRequest();
        request.setUsername("john.trainer");
        request.setFirstName("JohnUpdated");
        request.setLastName("TrainerUpdated");
        request.setIsActive(false);

        when(trainerRepository.findByUser_Username("john.trainer")).thenReturn(Optional.of(trainer));
        when(validator.validate(any(Trainer.class))).thenReturn(Set.of());
        when(trainerRepository.save(trainer)).thenReturn(trainer);

        Trainer result = trainerService.updateProfile("john.trainer", request);

        verify(userService).isAuthenticated("john.trainer");
        verify(userService).updateUserBasicInfo(user, "JohnUpdated", "TrainerUpdated", false);
        assertThat(result).isEqualTo(trainer);
    }

    @Test
    @DisplayName("updateProfile: throws NotFoundException if trainer missing")
    void updateProfile_trainerNotFound() {
        UpdateTrainerRequest request = new UpdateTrainerRequest();
        request.setUsername("john.trainer");

        when(trainerRepository.findByUser_Username("john.trainer")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainerService.updateProfile("john.trainer", request))
                .isInstanceOf(NotFoundException.class);

        verify(trainerRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProfile: throws ValidationException when validation fails")
    @SuppressWarnings("unchecked")
    void updateProfile_validationFails() {
        UpdateTrainerRequest request = new UpdateTrainerRequest();
        request.setUsername("john.trainer");
        request.setFirstName("JohnUpdated");
        request.setLastName("TrainerUpdated");
        request.setIsActive(true);

        when(trainerRepository.findByUser_Username("john.trainer")).thenReturn(Optional.of(trainer));

        ConstraintViolation<Trainer> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Invalid");
        when(validator.validate(any(Trainer.class))).thenReturn(Set.of(violation));

        assertThatThrownBy(() -> trainerService.updateProfile("john.trainer", request))
                .isInstanceOf(ValidationException.class);

        verify(trainerRepository, never()).save(any());
    }

    // ============ getUnassignedTrainers ============

    @Test
    @DisplayName("getUnassignedTrainers: returns available trainers")
    void getUnassignedTrainers_success() {
        Trainee trainee = new Trainee();
        trainee.setUser(user);

        Trainer trainer2 = Trainer.builder().id(2L).user(user).specialization(trainingType).build();

        when(traineeRepository.findByUser_Username("jane.doe")).thenReturn(Optional.of(trainee));
        when(trainerRepository.findAvailableTrainers("jane.doe")).thenReturn(List.of(trainer, trainer2));

        List<Trainer> result = trainerService.getUnassignedTrainers("jane.doe");

        verify(userService).isAuthenticated("jane.doe");
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getUnassignedTrainers: returns empty list")
    void getUnassignedTrainers_emptyList() {
        Trainee trainee = new Trainee();
        trainee.setUser(user);

        when(traineeRepository.findByUser_Username("jane.doe")).thenReturn(Optional.of(trainee));
        when(trainerRepository.findAvailableTrainers("jane.doe")).thenReturn(List.of());

        List<Trainer> result = trainerService.getUnassignedTrainers("jane.doe");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUnassignedTrainers: throws NotFoundException if trainee missing")
    void getUnassignedTrainers_traineeNotFound() {
        when(traineeRepository.findByUser_Username("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainerService.getUnassignedTrainers("unknown"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Trainee not found: unknown");

        verify(trainerRepository, never()).findAvailableTrainers(any());
    }
}