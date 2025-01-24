package dev.danvega.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ExamService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    // Constructeur avec injection de dépendance
    public ExamService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Méthode pour extraire le texte à partir d'un fichier PDF.
     */
    public String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            System.out.println("INFO: Successfully extracted text from PDF.");
            return text;
        } catch (IOException e) {
            System.err.println("ERROR: Failed to extract text from PDF. " + e.getMessage());
            throw new IOException("Erreur lors de l'extraction du texte du PDF.");
        }
    }

    /**
     * Méthode pour préparer le prompt à envoyer au modèle LLM.
     */
    public String preparePrompt(String content) {
        return """
            Generate exactly 5 multiple-choice questions (QCM) from the following course content. 
            Format your response as a valid JSON array:
            [
                {
                    "question": "Your question text here",
                    "choices": ["Choice 1", "Choice 2", "Choice 3"]
                }
            ]
            No additional text or explanations outside the JSON array.

            Course content:
            """ + content;
    }

    /**
     * Méthode pour appeler le modèle LLM avec le prompt préparé.
     */
    public String callLLM(String prompt) {
        try {
            System.out.println("INFO: Sending prompt to LLM...");
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            System.out.println("INFO: LLM response received.");
            return response;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to call LLM. Returning empty response. " + e.getMessage());
            return "[]"; // Retour vide en cas d'échec
        }
    }

    /**
     * Méthode pour nettoyer et valider la réponse JSON du modèle LLM.
     */
    public List<Map<String, Object>> sanitizeAndValidateResponse(String response) {
        System.out.println("DEBUG: Raw response from LLM: " + response);
        try {
            // Extraction stricte du JSON (recherche de crochets [ ])
            int startIndex = response.indexOf("[");
            int endIndex = response.lastIndexOf("]");
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                String sanitizedResponse = response.substring(startIndex, endIndex + 1).trim();
                System.out.println("DEBUG: Sanitized response: " + sanitizedResponse);

                // Validation et conversion en liste
                return objectMapper.readValue(sanitizedResponse, List.class);
            } else {
                throw new IOException("Aucun tableau JSON valide trouvé.");
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to parse JSON response. " + e.getMessage());
            // Retour d'une erreur formatée pour le frontend
            return List.of(
                    Map.of(
                            "question", "Erreur : Impossible de traiter la réponse générée.",
                            "choices", List.of("Veuillez réessayer avec un autre fichier PDF.")
                    )
            );
        }
    }

    /**
     * Méthode principale pour générer un examen à partir d'un fichier PDF.
     */
    public List<Map<String, Object>> generateExamFromPdf(MultipartFile file) {
        try {
            // Étape 1 : Extraction du texte depuis le PDF
            String courseContent = extractTextFromPdf(file);

            // Étape 2 : Préparation du prompt
            String prompt = preparePrompt(courseContent);

            // Étape 3 : Appel au modèle LLM
            String rawResponse = callLLM(prompt);

            // Étape 4 : Nettoyage et validation de la réponse
            return sanitizeAndValidateResponse(rawResponse);

        } catch (IOException e) {
            System.err.println("ERROR: PDF processing failed. " + e.getMessage());
            return List.of(
                    Map.of(
                            "question", "Erreur : Extraction du texte impossible.",
                            "choices", List.of("Le fichier PDF pourrait être corrompu ou mal formaté.")
                    )
            );
        }
    }
}
