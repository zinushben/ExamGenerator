package dev.danvega.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ExamService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public ExamService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
        this.objectMapper = new ObjectMapper();
    }

    public String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            System.out.println("INFO: Successfully extracted text from PDF.");
            return text;
        } catch (IOException e) {
            System.out.println("ERROR: Failed to extract text from PDF. " + e.getMessage());
            throw e;
        }
    }

    public String preparePrompt(String courseContent) {
        System.out.println("INFO: Preparing prompt for the LLM...");
        return """
            Based on the following course content, generate between 10 and 20 multiple-choice questions (QCM). 
            The response must be a valid JSON array strictly following this format:
            [
                {
                    "question": "Your question text here",
                    "choices": ["Choice 1", "Choice 2", "Choice 3"]
                },
                ...
            ]
            Do not include any additional text, explanations, or formatting outside the JSON array. 
            Here is the course content:
            """ + courseContent;
    }

    public String callLLM(String prompt) {
        System.out.println("INFO: Sending prompt to LLM...");
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        System.out.println("INFO: Received response from LLM. Response size: " + response.length() + " characters.");
        return response;
    }

    public Object sanitizeAndValidateResponse(String response) throws IOException {
        System.out.println("DEBUG: Raw response before sanitization: " + response);

        // Extraire uniquement la portion JSON de la réponse
        if (response.contains("[")) {
            int startIndex = response.indexOf("[");
            int endIndex = response.lastIndexOf("]");
            if (startIndex != -1 && endIndex != -1) {
                response = response.substring(startIndex, endIndex + 1);
            }
        }

        System.out.println("DEBUG: Sanitized response: " + response);
        // Valider que la réponse est un JSON valide
        return objectMapper.readValue(response.trim(), Object.class);
    }

    public Object generateExamFromPdf(MultipartFile file) throws IOException {
        // Étape 1 : Extraire le texte du PDF
        String courseContent = extractTextFromPdf(file);

        // Étape 2 : Préparer le prompt
        String prompt = preparePrompt(courseContent);

        // Étape 3 : Appeler le LLM
        String rawResponse = callLLM(prompt);

        // Étape 4 : Nettoyer et valider la réponse JSON
        return sanitizeAndValidateResponse(rawResponse);
    }
}

