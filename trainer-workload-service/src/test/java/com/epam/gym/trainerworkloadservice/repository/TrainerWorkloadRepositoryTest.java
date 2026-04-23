package com.epam.gym.trainerworkloadservice.repository;

import com.epam.gym.trainerworkloadservice.entity.TrainerWorkload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class TrainerWorkloadRepositoryTest {

    @Autowired
    private TrainerWorkloadRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private TrainerWorkload buildWorkload(String username, int year, int month, long duration) {
        return TrainerWorkload.builder()
                .trainerUsername(username)
                .trainerFirstName("First")
                .trainerLastName("Last")
                .trainerStatus(true)
                .year(year)
                .month(month)
                .trainingSummaryDuration(duration)
                .build();
    }

    @Test
    void findByTrainerUsernameAndYearAndMonth_found() {
        repository.save(buildWorkload("john.doe", 2024, 5, 60L));

        Optional<TrainerWorkload> result = repository
                .findByTrainerUsernameAndYearAndMonth("john.doe", 2024, 5);

        assertThat(result).isPresent();
        assertThat(result.get().getTrainingSummaryDuration()).isEqualTo(60L);
    }

    @Test
    void findByTrainerUsernameAndYearAndMonth_notFound() {
        Optional<TrainerWorkload> result = repository
                .findByTrainerUsernameAndYearAndMonth("unknown", 2024, 5);

        assertThat(result).isEmpty();
    }

    @Test
    void findByTrainerUsernameOrderByYearAscMonthAsc_returnsSorted() {
        repository.save(buildWorkload("john.doe", 2025, 3, 30L));
        repository.save(buildWorkload("john.doe", 2024, 5, 60L));
        repository.save(buildWorkload("john.doe", 2024, 2, 40L));

        List<TrainerWorkload> result = repository
                .findByTrainerUsernameOrderByYearAscMonthAsc("john.doe");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getYear()).isEqualTo(2024);
        assertThat(result.get(0).getMonth()).isEqualTo(2);
        assertThat(result.get(1).getYear()).isEqualTo(2024);
        assertThat(result.get(1).getMonth()).isEqualTo(5);
        assertThat(result.get(2).getYear()).isEqualTo(2025);
    }

    @Test
    void findByTrainerUsernameOrderByYearAscMonthAsc_emptyForUnknownTrainer() {
        List<TrainerWorkload> result = repository
                .findByTrainerUsernameOrderByYearAscMonthAsc("unknown");
        assertThat(result).isEmpty();
    }
}