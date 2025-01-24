package dev.danvega.controller;

import dev.danvega.service.AnswerVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exam")
public class AnsController {
    private final AnswerVerificationService verificationService;

    public AnsController(AnswerVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/verify-answers")
    public ResponseEntity<Object> verifyAnswers(@RequestBody List<Map<String, Object>> userAnswers) {
        try {
            // Appelle le service pour vérifier les réponses
            List<Map<String, Object>> results = verificationService.verifyUserAnswers(userAnswers);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error verifying answers: " + e.getMessage());
        }
    }
}
