package dev.danvega.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class AnswerVerificationService {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public AnswerVerificationService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
        this.objectMapper = new ObjectMapper();
    }

    public List<Map<String, Object>> verifyUserAnswers(List<Map<String, Object>> userAnswers) {
        // Préparation du prompt pour le LLM
        String prompt = prepareVerificationPrompt(userAnswers);

        // Envoi du prompt au LLM
        System.out.println("Sending verification prompt to LLM...");
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        System.out.println("Received verification response: " + response);

        // Nettoyage et parsing de la réponse
        String sanitizedResponse = sanitizeResponse(response);

        try {
            // Conversion de la réponse nettoyée en JSON
            return objectMapper.readValue(sanitizedResponse, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse verification response. " + e.getMessage());
        }
    }

    private String prepareVerificationPrompt(List<Map<String, Object>> userAnswers) {
        StringBuilder prompt = new StringBuilder("You are a professional exam checker.\n");
        prompt.append("The following is a list of questions, user answers, and possible choices. ");
        prompt.append("Check the user's answers, return the correct answers, and mark if their answers are correct or incorrect. ");
        prompt.append("The response must be in JSON format strictly following this structure:\n\n");
        prompt.append("[\n");
        prompt.append("{ \"question\": \"Question text\", \"correctAnswers\": [\"Correct Choice\"], \"isCorrect\": true/false }, ...\n");
        prompt.append("]\n\n");
        prompt.append("Here are the questions and answers:\n");

        userAnswers.forEach(answer -> {
            prompt.append("Question: ").append(answer.get("question")).append("\n");
            prompt.append("User Choices: ").append(answer.get("userChoices")).append("\n\n");
        });

        return prompt.toString();
    }

    private String sanitizeResponse(String response) {
        System.out.println("DEBUG: Raw response before sanitization: " + response);

        // Extraire uniquement la partie JSON à partir des crochets [ ]
        if (response.contains("[")) {
            int startIndex = response.indexOf("[");
            int endIndex = response.lastIndexOf("]");
            if (startIndex != -1 && endIndex != -1) {
                response = response.substring(startIndex, endIndex + 1);
            }
        }

        System.out.println("DEBUG: Sanitized response: " + response);
        return response.trim();
    }
}
