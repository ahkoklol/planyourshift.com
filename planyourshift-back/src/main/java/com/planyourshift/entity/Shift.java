package com.planyourshift.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "Shift")
public class Shift {

    @Id
    @Column(name = "shift_id")
    private String shiftId;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "store_id")
    private String storeId;

    private LocalDate day;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;
}