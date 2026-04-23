package com.epam.gym.trainerworkloadservice.repository;



import com.epam.gym.trainerworkloadservice.entity.TrainerWorkload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface TrainerWorkloadRepository extends JpaRepository<TrainerWorkload, Long> {

    Optional<TrainerWorkload> findByTrainerUsernameAndYearAndMonth(
            String trainerUsername, Integer year, Integer month);

    List<TrainerWorkload> findByTrainerUsernameOrderByYearAscMonthAsc(String trainerUsername);
}