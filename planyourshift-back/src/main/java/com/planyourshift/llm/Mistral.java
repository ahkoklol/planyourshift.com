package com.planyourshift.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planyourshift.entity.Employee;
import com.planyourshift.entity.Shift;
import com.planyourshift.entity.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.stereotype.Component;

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
            // Serialize the generated shifts to a JSON string for context
            scheduleJson = objectMapper.writeValueAsString(generatedShifts);
        } catch (Exception e) {
            log.error("Failed to serialize shifts for Mistral review", e);
            scheduleJson = "Serialization Error: Cannot review.";
        }

        // 2. Construct the prompt
        String promptText = """
            Analyze the following generated shift schedule for fairness and adherence to all stated preferences and constraints.
            Provide a brief, natural language summary (max 3 sentences) of the schedule's overall quality and mention any potential conflicts or imbalances.
            
            Generated Shifts (JSON):
            %s
            """.formatted(scheduleJson);

        try {
            // 3. Call the Mistral AI Model
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

}
