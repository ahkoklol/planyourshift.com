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
import java.util.Map;

@RestController
@RequestMapping("/schedules")
public class ScheduleController {

    private static final Logger log = LoggerFactory.getLogger(ScheduleController.class);

    @Autowired
    private SchedulingService schedulingService;

    @PostMapping("/{ownerId}/generate")
    public ResponseEntity<Map<String, List<Shift>>> generateSchedule(@PathVariable String ownerId, @RequestParam("weekStart") LocalDate weekStart) {
        Map<String, List<Shift>> proposedShifts = schedulingService.generateWeeklySchedule(ownerId);
        return ResponseEntity.ok(proposedShifts);
    }

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