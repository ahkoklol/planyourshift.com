package com.planyourshift.service;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import com.planyourshift.entity.*;
import com.planyourshift.repository.GeneratedScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import com.planyourshift.llm.LLM;

import static java.time.DayOfWeek.*;

@Service
public class SchedulingService {

    private static final Logger log = LoggerFactory.getLogger(SchedulingService.class);

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private StoreDayScheduleService storeDayScheduleService;

    @Autowired
    GeneratedScheduleRepository generatedScheduleRepository;

    @Autowired
    private LLM llm;

    @Autowired
    private ShiftService shiftService;

    /**
     * Orchestrates the shift generation process using LLM and OR-Tools.
     * @param ownerId The ID of the owner/brand.
     * @return A list of generated shifts.
     */
    public Map<String, List<Shift>> generateWeeklySchedule(String ownerId) {
        LocalDate startOfWeek = LocalDate.now().with(TemporalAdjusters.nextOrSame(MONDAY));
        log.info("Generating schedule for owner {} starting week {}", ownerId, startOfWeek);

        // 1. Data Collection
        List<Employee> employees = employeeService.getAllEmployeesByOwner(ownerId);
        List<Store> stores = storeService.getStores(ownerId);
        List<StoreDaySchedule> storeSchedules = storeDayScheduleService.getStoreDaySchedules(ownerId);

        if (employees.isEmpty() || stores.isEmpty()) {
            throw new IllegalArgumentException("Cannot generate schedule: No employees or stores found for ownerId " + ownerId);
        }

        // 2. LLM Pre-processing (Convert natural language to structured constraints)
        //var structuredConstraints = llm.parseSchedulingRequirements(employees, stores);

        // 3. OR-Tools Optimization (The OR-Tools logic goes here)
        // not done for now
        // var generatedShifts = runOrToolsSolver(employees, stores, storeSchedules, structuredConstraints, startOfWeek);
        // use LLM generation instead
        List<Shift> ownerRoster = llm.generateSchedule(employees, stores, storeSchedules, startOfWeek);
        llm.reviewSchedule(ownerRoster);

        shiftService.saveAllShifts(ownerRoster);

        // 6. Structure Output for Owner/Employee (Fulfilling user requirement)
        Map<String, List<Shift>> schedules = new HashMap<>();
        schedules.put("ownerRoster", ownerRoster);
        Map<String, List<Shift>> employeeSchedules = ownerRoster.stream()
                .collect(Collectors.groupingBy(Shift::getEmployeeId));
        // Add each employee's schedule to the final output map using their ID as the key
        schedules.putAll(employeeSchedules);

        log.info("Schedule generation complete");
        return schedules;
    }

    /**
     * Placeholder for the Google OR-Tools implementation.
     * This is where the core Constraint Programming logic resides.
     */
    private List<Shift> runOrToolsSolver(
            List<Employee> employees,
            List<Store> stores,
            List<StoreDaySchedule> storeSchedulesList,
            Map<String, Object> constraints,
            LocalDate startOfWeek) {

        // --- GOOGLE OR-TOOLS IMPLEMENTATION DETAILS ---
        // 1. Initialize the OR-Tools model (e.g., CP-SAT model).
        // 2. Define Variables (e.g., Boolean variable for (employee, store, time-slot)).
        //    * Variables: (Employee, StoreId, DayOfWeek, TimeSlot) -> True/False
        // 3. Apply Hard Constraints (Constraints that must be met):
        //    * Each employee works their required_hours (from Employee entity).
        //    * Store operating hours (from StoreDaySchedule entity).
        //    * Employee "cannot work" constraints (from LLM-processed data).
        //    * Ensure only one employee per time slot/store (if required, depending on needs).
        // 4. Apply Soft Constraints (Goals for Optimization):
        //    * Employee preferences ("prefers working at bagatelle") (from LLM-processed data).
        // 5. Define Objective Function (Minimize cost of soft constraints not met).
        // 6. Call the Solver (CpModel.solve()).
        // 7. Extract the solution and map it to a List<Shift> objects.
        // ----------------------------------------------

        return new ArrayList<>();
    }

    /**
     * Saves a GeneratedSchedule
     * @param generatedSchedule a GeneratedScheduleObject
     */
    public void saveGeneratedSchedule(GeneratedSchedule generatedSchedule) {
        generatedSchedule.setGeneratedScheduleId(UUID.randomUUID().toString());
        generatedScheduleRepository.save(generatedSchedule);
        log.info("Generated schedule {} saved", generatedSchedule.getGeneratedScheduleId());
    }
}