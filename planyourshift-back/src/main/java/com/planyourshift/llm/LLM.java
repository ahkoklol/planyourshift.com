package com.planyourshift.llm;

import com.planyourshift.entity.Employee;
import com.planyourshift.entity.Shift;
import com.planyourshift.entity.Store;
import com.planyourshift.entity.StoreDaySchedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface LLM {

    /**
     * Uses the LLM (Mistral) to convert natural language constraints/preferences
     * into a structured, machine-readable format for the OR-Tools solver.
     * @param employees List of employees with their raw constraints/preferences.
     * @param stores List of all available stores (locations).
     * @return A map structure (or a specific DTO) representing structured scheduling rules.
     */
    Map<String, Object> parseSchedulingRequirements(List<Employee> employees, List<Store> stores);

    /**
     * Optionally, the LLM can also be used to evaluate the final schedule
     * or provide a natural language summary/justification.
     * @param generatedShifts The shifts produced by the OR-Tools.
     * @return A natural language summary or score.
     */
    String reviewSchedule(List<Shift> generatedShifts);

    /**
     * Uses the LLM (Mistral) to generate a complete list of shifts for the week.
     * This replaces the optimization solver logic.
     * @param employees List of employees, their constraints, and preferences.
     * @param stores List of all available stores and their names.
     * @param storeSchedules List of store operating hours (nullable times indicate closed days).
     * @param startOfWeek The start date (Monday) of the scheduling week.
     * @return A list of generated Shift objects (the Owner's Roster).
     */
    List<Shift> generateSchedule(List<Employee> employees, List<Store> stores,
                                 List<StoreDaySchedule> storeSchedules, LocalDate startOfWeek);
}
