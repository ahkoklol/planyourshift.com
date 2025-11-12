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

    // --- STATIC LOADER ---
    static {
        try {
            com.google.ortools.Loader.loadNativeLibraries();
            log.info("OR-Tools native libraries loaded successfully.");
        } catch (UnsatisfiedLinkError e) {
            log.error("Failed to load OR-Tools native libraries", e);
            throw e; // fail fast if OR-Tools cannot load
        }
    }

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

    public List<Shift> solveRoaster(String ownerId) {
        CpModel model = new CpModel();

        // constraints: number of employees, number of stores, each store open and close time for each day of week, employee required hours, employee max shift time
        List<Employee> employees = employeeService.getAllEmployeesByOwner(ownerId);
        int employeeCount = employees.size();
        List<Store> stores = storeService.getStores(ownerId);
        List<StoreDaySchedule> storeDaySchedules = storeDayScheduleService.getStoreDaySchedules(ownerId);

        // Build weekly store schedule map: DAY_NAME -> (openTime -> closeTime)
        Map<DayOfWeek, Map<LocalTime, LocalTime>> weeklyStoreSchedule = new HashMap<>();
        storeDaySchedules.forEach(storeDaySchedule -> {
            Map<LocalTime, LocalTime> operatingHours = new HashMap<>();
            operatingHours.put(storeDaySchedule.getOpenTime(), storeDaySchedule.getCloseTime());
            weeklyStoreSchedule.put(storeDaySchedule.getDayOfWeek(), operatingHours);
        });

        // Required hours per employee (weekly)
        Map<Employee, Double> requiredHours = new HashMap<>();
        employees.forEach(employee -> requiredHours.put(employee, employee.getRequiredHours()));

        // Max shift time per employee (hours)
        Map<Employee, Double> maxShiftTime = new HashMap<>();
        employees.forEach(employee -> maxShiftTime.put(employee, employee.getMaxShiftTime()));

        // ------------------------------------------

        // 1. SETUP CONSTANTS AND MAPPINGS
        final int MINUTES_PER_INTERVAL = 15;
        final int MINUTES_PER_HOUR = 60;
        final int INTERVALS_PER_HOUR = MINUTES_PER_HOUR / MINUTES_PER_INTERVAL; // 4

        final int DAYS_IN_WEEK = 7;
        String[] dayNames = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        int maxIntervalsPerDay = 24 * INTERVALS_PER_HOUR; // 96

        // Index Maps
        Map<Employee, Integer> employeeToIndex = new HashMap<>();
        for (int i = 0; i < employeeCount; i++) employeeToIndex.put(employees.get(i), i);

        Map<String, Integer> dayToIndex = new HashMap<>();
        for (int i = 0; i < DAYS_IN_WEEK; i++) dayToIndex.put(dayNames[i], i);

        // 2. PROCESS STORE SCHEDULE AND DEFINE STAFFING REQUIREMENTS
        int[] numIntervalsForDay = new int[DAYS_IN_WEEK];
        int[][] requiredStaff = new int[DAYS_IN_WEEK][maxIntervalsPerDay];
        LocalTime[] openTimePerDay = new LocalTime[DAYS_IN_WEEK];
        LocalTime[] closeTimePerDay = new LocalTime[DAYS_IN_WEEK];

        for (int d = 0; d < DAYS_IN_WEEK; d++) {
            String dayName = dayNames[d];
            Map<LocalTime, LocalTime> hoursMap = weeklyStoreSchedule.get(dayName);

            if (hoursMap != null && !hoursMap.isEmpty()) {
                LocalTime openTime = hoursMap.keySet().iterator().next();
                LocalTime closeTime = hoursMap.get(openTime);

                if (openTime != null && closeTime != null && closeTime.isAfter(openTime)) {
                    long minutesOpen = java.time.Duration.between(openTime, closeTime).toMinutes();
                    numIntervalsForDay[d] = (int) (minutesOpen / MINUTES_PER_INTERVAL);

                    // store for mapping solution back to times
                    openTimePerDay[d] = openTime;
                    closeTimePerDay[d] = closeTime;

                    // --- STAFFING LEVEL PLACEHOLDER: set to 2 for each slot (replace with real demand) ---
                    for (int t = 0; t < numIntervalsForDay[d]; t++) requiredStaff[d][t] = 2;
                }
            }
        }

        // 3. DEFINE DECISION VARIABLES
        IntVar[][][] isWorking = new IntVar[employeeCount][DAYS_IN_WEEK][maxIntervalsPerDay];
        for (int e = 0; e < employeeCount; e++) {
            for (int d = 0; d < DAYS_IN_WEEK; d++) {
                for (int t = 0; t < numIntervalsForDay[d]; t++) {
                    isWorking[e][d][t] = model.newBoolVar(String.format("E%d_D%d_T%d", e, d, t));
                }
            }
        }

        // 4. ADD CONSTRAINTS
        // A) Required hours per employee per week (>=)
        for (int e = 0; e < employeeCount; e++) {
            Employee emp = employees.get(e);
            double reqHours = requiredHours.getOrDefault(emp, 0.0);
            int reqIntervals = (int) Math.ceil(reqHours * INTERVALS_PER_HOUR);

            // sum of all intervals across the week for this employee
            List<IntVar> allSlots = new ArrayList<>();
            for (int d = 0; d < DAYS_IN_WEEK; d++) {
                for (int t = 0; t < numIntervalsForDay[d]; t++) {
                    allSlots.add(isWorking[e][d][t]);
                }
            }
            if (!allSlots.isEmpty()) {
                // Employee must work at least required intervals per week
                model.addGreaterOrEqual(LinearExpr.sum(allSlots.toArray(new IntVar[0])), reqIntervals);
            }
        }

        // D) SHIFT CONTINUITY: Single continuous shift per day - and track Start/End/Length for extraction
        IntVar[][] start = new IntVar[employeeCount][DAYS_IN_WEEK];
        IntVar[][] endPlusOne = new IntVar[employeeCount][DAYS_IN_WEEK];
        IntVar[][] lengthVar = new IntVar[employeeCount][DAYS_IN_WEEK];
        BoolVar[][] isWorkingTodayArr = new BoolVar[employeeCount][DAYS_IN_WEEK];

        for (int e = 0; e < employeeCount; e++) {
            Employee employee = employees.get(e);
            double maxShiftHours = maxShiftTime.getOrDefault(employee, 8.0);
            int maxShiftIntervals = (int) (maxShiftHours * INTERVALS_PER_HOUR);

            for (int d = 0; d < DAYS_IN_WEEK; d++) {
                int maxIntervals = numIntervalsForDay[d];
                if (maxIntervals == 0) {
                    // create trivial variables for consistency (length 0, not working)
                    lengthVar[e][d] = model.newIntVar(0, 0, String.format("Length_E%d_D%d", e, d));
                    start[e][d] = model.newIntVar(0, 0, String.format("Start_E%d_D%d", e, d));
                    endPlusOne[e][d] = model.newIntVar(0, 0, String.format("End_E%d_D%d", e, d));
                    isWorkingTodayArr[e][d] = model.newBoolVar(String.format("IsWorking_E%d_D%d", e, d));
                    // force not working
                    model.addEquality(lengthVar[e][d], 0).onlyEnforceIf(isWorkingTodayArr[e][d].not());
                    continue;
                }

                // Variables
                lengthVar[e][d] = model.newIntVar(0, maxShiftIntervals, String.format("Length_E%d_D%d", e, d));
                isWorkingTodayArr[e][d] = model.newBoolVar(String.format("IsWorking_E%d_D%d", e, d));
                start[e][d] = model.newIntVar(0, maxIntervals - 1, String.format("Start_E%d_D%d", e, d));
                endPlusOne[e][d] = model.newIntVar(0, maxIntervals, String.format("End_E%d_D%d", e, d));

                // collect working slots for day
                List<IntVar> workingSlotsToday = new ArrayList<>();
                for (int t = 0; t < maxIntervals; t++) {
                    if (isWorking[e][d][t] != null) workingSlotsToday.add(isWorking[e][d][t]);
                }

                // 1) total working slots equals length
                model.addEquality(LinearExpr.sum(workingSlotsToday.toArray(new IntVar[0])), lengthVar[e][d]);

                // 2) min and max over boolean slots to get first and last (min returns 0 if all 0)
                // addMinEquality / addMaxEquality require a non-empty array
                model.addMinEquality(start[e][d], workingSlotsToday.toArray(new IntVar[0]));
                model.addMaxEquality(endPlusOne[e][d], workingSlotsToday.toArray(new IntVar[0]));

                // 3) continuity: end = start + length (only if working today)
                model.addEquality(endPlusOne[e][d], LinearExpr.sum(new IntVar[]{start[e][d], lengthVar[e][d]}))
                        .onlyEnforceIf(isWorkingTodayArr[e][d]);

                // 4) link boolean to length
                model.addGreaterOrEqual(lengthVar[e][d], 1).onlyEnforceIf(isWorkingTodayArr[e][d]);
                model.addEquality(lengthVar[e][d], 0).onlyEnforceIf(isWorkingTodayArr[e][d].not());
            }
        }

        // C) STAFFING COVERAGE
        for (int d = 0; d < DAYS_IN_WEEK; d++) {
            for (int t = 0; t < numIntervalsForDay[d]; t++) {
                List<IntVar> employeesWorkingThisSlot = new ArrayList<>();
                for (int e = 0; e < employeeCount; e++) employeesWorkingThisSlot.add(isWorking[e][d][t]);

                int requiredLevel = requiredStaff[d][t];
                if (requiredLevel > 0) {
                    model.addGreaterOrEqual(LinearExpr.sum(employeesWorkingThisSlot.toArray(new IntVar[0])), requiredLevel);
                }
            }
        }

        // Optional: small objective to minimize total over-staffing (not required) - omitted for clarity.

        // 5. SOLVE
        CpSolver solver = new CpSolver();
        solver.getParameters().setLogSearchProgress(false);
        solver.getParameters().setLinearizationLevel(0);

        CpSolverStatus status = solver.solve(model);

        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            // extract using start/length variables
            return extractShiftsFromSolution(solver, employees, dayNames, numIntervalsForDay, openTimePerDay, start, lengthVar, isWorkingTodayArr);
        } else {
            System.err.println("No feasible solution found. Status: " + status);
            return Collections.emptyList();
        }
    }

    /**
     * Helper: extract shifts from solver values
     * NOTE: adjust Shift construction to match your domain class. Here I assume:
     *   public Shift(String employeeId, String dayOfWeek, LocalTime startTime, LocalTime endTime)
     * If your Shift class differs, replace the construction/setting section accordingly.
     */
    private List<Shift> extractShiftsFromSolution(
            CpSolver solver,
            List<Employee> employees,
            String[] dayNames,
            int[] numIntervalsForDay,
            LocalTime[] openTimePerDay,
            IntVar[][] start,
            IntVar[][] lengthVar,
            BoolVar[][] isWorkingTodayArr
    ) {
        final int MINUTES_PER_INTERVAL = 15;
        List<Shift> result = new ArrayList<>();

        for (int e = 0; e < employees.size(); e++) {
            Employee emp = employees.get(e);
            for (int d = 0; d < dayNames.length; d++) {
                if (numIntervalsForDay[d] == 0) continue;

                // if the solver says the employee works this day
                boolean worksToday = solver.booleanValue(isWorkingTodayArr[e][d]);
                if (!worksToday) continue;

                long startInterval = solver.value(start[e][d]);
                long lengthIntervals = solver.value(lengthVar[e][d]);

                if (lengthIntervals <= 0) continue; // nothing to emit

                long endIntervalExcl = startInterval + lengthIntervals; // exclusive

                // Map intervals to LocalTime using the day's openTime
                LocalTime dayOpen = openTimePerDay[d];
                if (dayOpen == null) {
                    // Fallback: assume midnight if not provided
                    dayOpen = LocalTime.MIDNIGHT;
                }

                LocalTime shiftStart = dayOpen.plusMinutes(startInterval * MINUTES_PER_INTERVAL);
                LocalTime shiftEnd = dayOpen.plusMinutes(endIntervalExcl * MINUTES_PER_INTERVAL);

                // Construct Shift object - adapt to your Shift model
                // Example constructor: Shift(String employeeId, String dayOfWeek, LocalTime start, LocalTime end)
                Shift shift = new Shift();
                shift.setShiftId(UUID.randomUUID().toString());
                shift.setEmployeeId(emp.getEmployeeId());
                shift.setDay(DayOfWeek.valueOf(dayNames[d]));
                shift.setStartTime(shiftStart);
                shift.setEndTime(shiftEnd);

                result.add(shift);
            }
        }
        return result;
    }
}