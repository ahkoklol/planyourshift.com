package com.planyourshift.llm;

import com.planyourshift.entity.Employee;
import com.planyourshift.entity.Shift;
import com.planyourshift.entity.Store;

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
}
