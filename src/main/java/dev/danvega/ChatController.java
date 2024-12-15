package dev.danvega;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exam")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/generate-from-pdf")
    public ResponseEntity<Object> generateExamFromPdf(@RequestParam("file") MultipartFile file) throws IOException {
        System.out.println("INFO: Received a request to generate exam from PDF.");

        // Étape 1 : Extraire le texte du PDF
        String courseContent = extractTextFromPdf(file);
        System.out.println("INFO: Extracted text from PDF. Content size: " + courseContent.length() + " characters.");

        // Étape 2 : Préparer le prompt
        String prompt = preparePrompt(courseContent);
        System.out.println("INFO: Prepared prompt for the LLM.");

        // Étape 3 : Appeler le LLM
        System.out.println("INFO: Sending prompt to LLM...");
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        System.out.println("INFO: Received response from LLM. Response size: " + response.length() + " characters.");

        // Étape 4 : Valider et nettoyer la réponse JSON
        try {
            String sanitizedResponse = sanitizeResponse(response);
            ObjectMapper objectMapper = new ObjectMapper();
            Object jsonResponse = objectMapper.readValue(sanitizedResponse, Object.class); // Valider le JSON
            System.out.println("INFO: Successfully validated JSON response.");
            return ResponseEntity.ok(jsonResponse); // Renvoyer directement le JSON
        } catch (Exception e) {
            System.out.println("ERROR: Failed to process the response. " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid JSON response: " + e.getMessage());
        }
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
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

    private String preparePrompt(String courseContent) {
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

    private String sanitizeResponse(String response) {
        System.out.println("DEBUG: Raw response before sanitization: " + response);

        // Si la réponse contient du texte avant/après le JSON, extraire uniquement la portion JSON
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
