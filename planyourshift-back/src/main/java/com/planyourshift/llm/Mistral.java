package com.planyourshift.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.stereotype.Component;

@Component
public class Mistral implements LLM {

    private static final Logger log = LoggerFactory.getLogger(Mistral.class);

    private final MistralAiChatModel chatModel;

    public Mistral(MistralAiChatModel chatModel) {
        this.chatModel = chatModel;
    }
}
