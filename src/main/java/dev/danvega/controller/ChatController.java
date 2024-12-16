package dev.danvega.controller;

import dev.danvega.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/exam")
public class ChatController {

    private final ExamService examService;

    @Autowired
    public ChatController(ExamService examService) {
        this.examService = examService;
    }

    @PostMapping("/generate-from-pdf")
    public ResponseEntity<Object> generateExamFromPdf(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("INFO: Received a request to generate exam from PDF.");
            Object jsonResponse = examService.generateExamFromPdf(file);
            System.out.println("INFO: Successfully generated exam.");
            return ResponseEntity.ok(jsonResponse);
        } catch (IOException e) {
            System.out.println("ERROR: Failed to process PDF. " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        }
    }
}
