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
            Return ONLY a single JSON object with the keys 'hardConstraints' and 'softConstraints'.
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
            log.info("Mistral Response (truncated): {}", content.substring(0, Math.min(500, content.length())));

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

        // 1. Prepare ALL input data as a single JSON object for the LLM
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

        // 2. Define the expected output structure for the LLM
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

        // 3. Construct the prompt
        String promptText = """
            You are an expert shift manager. Your task is to generate an optimal weekly schedule for the week starting %s.

            Strict Rules:
            1. Employee's total scheduled hours must be GREATER THAN or EQUAL TO their 'required_hours' where possible.
            2. All generated shifts MUST fall within the 'open_time' and 'close_time' specified in 'storeSchedules'. If open_time/close_time is null, the store is closed on that day.
            3. Respect all natural language 'constraints' (hard rules, e.g., 'cannot work Sunday').
            4. Prioritize all natural language 'preferences' (soft rules, e.g., 'prefers Bagatelle store').
            5. Ensure only ONE employee is scheduled per store per time slot.
            6. All generated shifts must have a duration of 8 hours.

            Return ONLY a single JSON array of Shift objects matching the following schema. Do not include any text, reasoning, or markdown outside the JSON array itself.

            Input Data:
            %s

            Output Schema (JSON Array of Shifts):
            %s
            """.formatted(startOfWeek, inputJson, outputSchema);

        String content;
        try {
            // 4. Call the Mistral AI Model
            ChatResponse response = chatModel.call(new Prompt(promptText));
            content = response.getResult().getOutput().getText();
            assert content != null;
            log.info("Mistral Response (truncated): {}", content.substring(0, Math.min(1000, content.length())));

            // 5. Deserialize the LLM's JSON response back into a List<Shift>
            TypeReference<List<Shift>> listType = new TypeReference<>() {};
            return objectMapper.readValue(content, listType);

        } catch (Exception e) {
            log.error("Error calling Mistral or parsing schedule response: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Strips markdown code blocks (```json\n...\n```) from the LLM response.
     */
    private String cleanJsonOutput(String response) {
        if (response == null) {
            return "[]";
        }
        // Efficiently strip leading/trailing whitespace and the ```json ... ``` wrapper
        response = response.strip();
        if (response.startsWith("```")) {
            // Find the index of the first actual JSON character (after ```json\n)
            int start = response.indexOf('{');
            if (start == -1) {
                start = response.indexOf('[');
            }

            // Find the index of the last actual JSON character (before ```)
            int end = response.lastIndexOf("```");

            // If a start and end marker are found, extract the clean JSON substring
            if (start != -1 && end > start) {
                return response.substring(start, end).strip();
            }
        }
        return response;
    }

}
