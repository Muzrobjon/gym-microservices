package com.epam.gym.trainerworkloadservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trainer_workload")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerWorkload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trainer_username", nullable = false)
    private String trainerUsername;

    @Column(name = "trainer_first_name", nullable = false)
    private String trainerFirstName;

    @Column(name = "trainer_last_name", nullable = false)
    private String trainerLastName;

    @Column(name = "trainer_status", nullable = false)
    private Boolean trainerStatus;

    // TODO:
    //  This is a flat model, compared to the nested one specified in the task
    //  It's not only denormalizes database with one row per trainer per month, but also
    //  overcomplicates grouping and summing up the data in your service.
    //  Please update according to the requirements
    @Column(name = "training_year", nullable = false)
    private Integer year;

    @Column(name = "training_month", nullable = false)
    private Integer month;

    @Column(name = "training_summary_duration", nullable = false)
    private Long trainingSummaryDuration;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrainerWorkload that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
