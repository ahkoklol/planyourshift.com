package com.planyourshift.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "GeneratedSchedule")
public class GeneratedSchedule {

    @Id
    @Column(name = "generated_schedule_id")
    private String generatedScheduleId;

    @Column(name = "store_id")
    private String storeId;

    private LocalDate week;
}
