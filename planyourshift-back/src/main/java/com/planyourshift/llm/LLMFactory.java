package com.planyourshift.llm;

import org.springframework.stereotype.Component;

@Component
public class LLMFactory {

    private final Mistral mistral;

    public LLMFactory(Mistral mistral) {
        this.mistral = mistral;
    }

    /**
     * Creates an LLM
     * @param model the name of the model
     * @return the model
     */
    public LLM getLLM(String model) {
        if ("mistral".equalsIgnoreCase(model)) {
            return mistral;
        }
        throw new IllegalArgumentException("Unsupported LLM model: " + model);
    }
}
