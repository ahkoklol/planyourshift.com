package com.planyourshift.controller;

import com.planyourshift.entity.Shift;
import com.planyourshift.service.SchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedules")
public class ScheduleController {

    private static final Logger log = LoggerFactory.getLogger(ScheduleController.class);

    @Autowired
    private SchedulingService schedulingService;

    /**
     * Generates a weekly shift schedule for an owner's brand/stores.
     * The generated schedule includes shifts for all employees across all stores.
     * @param ownerId The owner/brand ID for which to generate the schedule.
     * @param weekStart The Monday date of the week to schedule (e.g., 2024-10-28).
     * @return The complete list of proposed shifts (the owner's roster).
     */
    @PostMapping("/{ownerId}/generate")
    public ResponseEntity<List<Shift>> generateSchedule(@PathVariable String ownerId, @RequestParam("weekStart") LocalDate weekStart) {
        List<Shift> proposedShifts = schedulingService.generateWeeklySchedule(ownerId);
        return ResponseEntity.ok(proposedShifts);
    }

    /**
     * Endpoint to get a specific employee's schedule for a week.
     * This uses the finalized shifts stored in the database.
     */
    @GetMapping("/{ownerId}/employee/{employeeId}")
    public ResponseEntity<List<Shift>> getEmployeeSchedule(
            @PathVariable String ownerId,
            @PathVariable String employeeId,
            @RequestParam("weekStart") LocalDate weekStart) {

        // TODO: Implement a repository query to find all Shifts for this employee/week.
        // This provides the second required output: the employee's personal schedule.
        return ResponseEntity.notFound().build();
    }

    // You would add more endpoints here for:
    // - Confirmation of the proposed schedule
    // - Export (CSV/PDF)
    // - Email notifications
}