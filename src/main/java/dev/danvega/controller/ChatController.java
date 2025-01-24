package dev.danvega.controller;

import dev.danvega.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exam")
public class ChatController {

    private final ExamService examService;

    // Injection de ExamService
    @Autowired
    public ChatController(ExamService examService) {
        this.examService = examService;
    }

    // Endpoint principal pour générer un examen
    @PostMapping("/generate-from-pdf")
    public ResponseEntity<Object> generateExamFromPdf(@RequestParam("file") MultipartFile file) {
        try {
            // Appel de la méthode principale de ExamService
            List<Map<String, Object>> questions = examService.generateExamFromPdf(file);
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Erreur lors du traitement du fichier PDF.", "details", e.getMessage())
            );
        }
    }
}
