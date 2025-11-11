package com.planyourshift.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planyourshift.entity.Employee;
import com.planyourshift.entity.Shift;
import com.planyourshift.entity.Store;
import com.planyourshift.entity.StoreDaySchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class Mistral implements LLM {

    private static final Logger log = LoggerFactory.getLogger(Mistral.class);

    private final MistralAiChatModel chatModel;

    private final ObjectMapper objectMapper;

    public Mistral(MistralAiChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    /**
     * Uses Mistral to convert natural language constraints and preferences
     * into a structured Map (JSON) for the OR-Tools solver.
     */
    @Override
    public Map<String, Object> parseSchedulingRequirements(List<Employee> employees, List<Store> stores) {
        log.info("Mistral: Parsing scheduling requirements for {} employees and {} stores.", employees.size(), stores.size());

        // 1. Prepare ALL input data as a single JSON object for the LLM
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("employees", employees);
        inputData.put("stores", stores);

        String inputJson;
        try {
            // Serialize the full input data structure to a JSON string
            inputJson = objectMapper.writeValueAsString(inputData);
        } catch (Exception e) {
            log.error("Failed to serialize input data for Mistral", e);
            throw new RuntimeException("Data serialization error.", e);
        }

        // 2. Construct the prompt, asking for structured JSON output
        String promptText = """
            You are an expert scheduler. Analyze the following employee requirements, constraints, preferences, and store data.
            Your task is to convert the natural language 'constraints' and 'preferences' into a concise, structured JSON format\s
            that can be directly consumed by an optimization solver (Google OR-Tools).
           \s
            Return ONLY a single JSON object with the keys 'hardConstraints' and 'softConstraints'. Do not include any text, reasoning, or markdown outside the JSON object itself.
            The value for each key must be a list of structured rule objects.

            Example Output Structure:
            {
                "hardConstraints": [
                    {"employeeId": "uuid-1", "type": "dayOff", "day": "SUNDAY"},
                    {"employeeId": "uuid-2", "type": "maxHours", "value": 40}
                ],
                "softConstraints": [
                    {"employeeId": "uuid-1", "type": "preferStore", "storeId": "store-bagatelle", "weight": 5}
                ]
            }

            Input Data:
            %s
           \s""".formatted(inputJson);

        String content;
        try {
            // 3. Call the Mistral AI Model
            ChatResponse response = chatModel.call(new Prompt(promptText));
            content = response.getResult().getOutput().getText();
            assert content != null;

            // FIX: Clean the output before parsing
            content = cleanJsonOutput(content);
            log.info("Mistral Clean JSON Response (truncated): {}", content.substring(0, Math.min(500, content.length())));

            // 4. Deserialize the LLM's structured JSON response back into a Java Map
            TypeReference<Map<String, Object>> mapType = new TypeReference<>() {};
            return objectMapper.readValue(content, mapType);

        } catch (Exception e) {
            log.error("Error calling Mistral or parsing response: {}", e.getMessage());
            // Return empty map on failure, forcing the scheduling service to handle the missing constraints
            return Map.of();
        }
    }

    /**
     * Uses Mistral to review the generated shifts and provide a natural language summary.
     */
    @Override
    public String reviewSchedule(List<Shift> generatedShifts) {
        log.info("Mistral: Reviewing generated schedule with {} shifts.", generatedShifts.size());

        String scheduleJson;
        try {
            scheduleJson = objectMapper.writeValueAsString(generatedShifts);
        } catch (Exception e) {
            log.error("Failed to serialize shifts for Mistral review", e);
            scheduleJson = "Serialization Error: Cannot review.";
        }

        String promptText = """
            Analyze the following generated shift schedule for fairness and adherence to all stated preferences and constraints.
            Provide a brief, natural language summary (max 3 sentences) of the schedule's overall quality and mention any potential conflicts or imbalances.

            Generated Shifts (JSON):
            %s
            """.formatted(scheduleJson);

        try {
            ChatResponse response = chatModel.call(new Prompt(promptText));
            String content = response.getResult().getOutput().getText();
            assert content != null;
            log.info("Mistral Review (truncated): {}", content.substring(0, Math.min(500, content.length())));
            return content;
        } catch (Exception e) {
            log.error("Error calling Mistral for schedule review: {}", e.getMessage());
            return "Review failed: Could not communicate with the scheduling AI.";
        }
    }

    /**
     * Generates a complete weekly shift schedule based on all constraints and preferences.
     */
    @Override
    public List<Shift> generateSchedule(
            List<Employee> employees,
            List<Store> stores,
            List<StoreDaySchedule> storeSchedules,
            LocalDate startOfWeek) {

        log.info("Mistral: Generating weekly schedule starting {}", startOfWeek);

        // --- DYNAMICALLY PULL REQUIRED HOURS FROM EMPLOYEE LIST ---
        double totalRequiredHours = employees.stream()
                .mapToDouble(Employee::getRequiredHours)
                .sum();

        String employeeRequirementsSummary = employees.stream()
                .map(e -> String.format("{ID: %s, Required: %.1f hours, Constraints: '%s', Preferences: '%s'}",
                        e.getEmployeeId(), e.getRequiredHours(), e.getConstraints(), e.getPreferences()))
                .collect(Collectors.joining("\n- ", "- ", ""));
        // ---------------------------------------------------------

        // 3. Prepare ALL input data as a single JSON object for the LLM
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("employees", employees);
        inputData.put("stores", stores);
        inputData.put("storeSchedules", storeSchedules);
        inputData.put("schedulingWeekStart", startOfWeek);

        String inputJson;
        try {
            inputJson = objectMapper.writeValueAsString(inputData);
        } catch (Exception e) {
            log.error("Failed to serialize input data for Mistral", e);
            throw new RuntimeException("Data serialization error.", e);
        }

        // 4. Define the expected output structure for the LLM
        String outputSchema = """
            [
              {
                "shiftId": "string (Unique ID)",
                "employeeId": "string (REQUIRED, must match an input employeeId)",
                "storeId": "string (REQUIRED, must match an input storeId)",
                "day": "LocalDate (e.g., 2025-11-17)",
                "startTime": "LocalTime (e.g., 09:00:00)",
                "endTime": "LocalTime (e.g., 17:00:00)"
              },
              ...
            ]
            """;

        // 5. Construct the final prompt using dynamic values
        String promptText = """
            You are an expert shift manager. Your task is to generate an optimal weekly schedule for the week starting %s.

            Rules and Guidelines:
            1. **ABSOLUTE PRIORITY: EXACT HOURS**: Each employee **MUST** be scheduled for **EXACTLY** their 'required_hours' (listed below). The total scheduled hours for the entire staff must equal %.1f hours. **This numerical target MUST be hit.**
            2. **CRITICAL TIME COVERAGE**: Shifts **MUST** be generated for **ALL 7 DAYS** if store schedules are provided. The total time covered by shifts each day **MUST span the ENTIRE** 'open_time' to 'close_time' window specified in the 'storeSchedules' input (e.g., 09:00 to 22:00 must be covered continuously).
            3. Respect all natural language 'constraints' (hard rules).
            4. Prioritize all natural language 'preferences' (soft rules).
            5. **CRUCIAL: STACK SHIFTS**: To meet the exact hour targets (Rule 1), you **MUST** assign **MULTIPLE SHIFT SEGMENTS** (with or without breaks between them) to the **SAME EMPLOYEE** on the same day. **DO NOT rotate segments** if it prevents hitting the 45-hour quota. Focus on stacking shifts per employee until they reach their required 45 hours. Overlapping shifts are expected.
            6. Shift Length: Shifts should be segmented into 4-10 hour blocks. Use precise minute values if necessary to hit the exact required hours.
            7. **STRICT REQUIREMENT:** Output ONLY the raw JSON array. Do not include ANY text, commentary, preambles, apologies, or markdown (like ```json) outside the JSON array itself.

            Employee-Specific Requirements:
            %s

            Input Data (Full JSON):
            %s

            Output Schema (JSON Array of Shifts):
            %s
            """.formatted(startOfWeek, totalRequiredHours, employeeRequirementsSummary, inputJson, outputSchema);

        String content;
        try {
            // 6. Call the Mistral AI Model
            ChatResponse response = chatModel.call(new Prompt(promptText));
            content = response.getResult().getOutput().getText();
            assert content != null;

            // FIX: Clean the output before parsing
            content = cleanJsonOutput(content);
            log.info("Mistral Clean JSON Response (truncated): {}", content.substring(0, Math.min(1000, content.length())));

            // 7. Deserialize the LLM's JSON response back into a List<Shift>
            TypeReference<List<Shift>> listType = new TypeReference<>() {};
            return objectMapper.readValue(content, listType);

        } catch (Exception e) {
            log.error("Error calling Mistral or parsing schedule response: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Strips markdown code blocks (e.g., ```json\n...\n```), preambles, and postambles
     * from the raw LLM response to ensure the result is pure JSON.
     *
     * @param response The raw string response from the LLM.
     * @return The cleaned string containing only raw JSON (or "[]" if no valid JSON found).
     */
    private String cleanJsonOutput(String response) {
        if (response == null) {
            return "[]";
        }

        // 1. Remove all known delimiters and trim
        String cleanResponse = response.replaceAll("```json|```", "").trim();

        // 2. Find the index of the first actual JSON start character: '[' or '{'
        int startArray = cleanResponse.indexOf('[');
        int startObject = cleanResponse.indexOf('{');

        int start = -1;
        int end = -1;

        // Prioritize the outer-most structure (which should be the array '[]' for shifts)
        if (startArray != -1 && (startObject == -1 || startArray < startObject)) {
            // Found array. Try to find its matching end ']'
            start = startArray;
            end = cleanResponse.lastIndexOf(']');
        } else if (startObject != -1) {
            // Found object. Try to find its matching end '}'
            start = startObject;
            end = cleanResponse.lastIndexOf('}');
        }

        // 3. Extract and return the clean JSON substring
        if (start != -1 && end != -1 && end > start) {
            return cleanResponse.substring(start, end + 1).trim();
        }

        // Fallback to safe empty JSON array if no valid structure was found
        log.warn("Failed to find valid JSON structure in cleaned LLM response.");
        return "[]";
    }
}