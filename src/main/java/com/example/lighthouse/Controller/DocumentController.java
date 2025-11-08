// src/main/java/com/example/lighthouse/controller/DocumentController.java
package com.example.lighthouse.controller;

import com.example.lighthouse.Model.Document;
import com.example.lighthouse.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "http://localhost:5173")
public class DocumentController {

    @Autowired
    private DocumentRepository documentRepository;

    // Get all documents
    @GetMapping
    public List<Document> getAllDocuments() {
        return documentRepository.findAllByOrderByCreatedAtDesc();
    }

    // Get single document
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable String id) {
        return documentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Create document
    @PostMapping
    public Document createDocument(@RequestBody Map<String, String> request) {
        Document doc = new Document();
        doc.setTitle(request.get("title"));
        doc.setContent(request.get("content"));
        doc.setCategory(request.getOrDefault("category", "general"));
        return documentRepository.save(doc);
    }

    // Delete document
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id) {
        documentRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // Bulk upload (for demo)
    @PostMapping("/bulk")
    public List<Document> bulkUpload(@RequestBody List<Map<String, String>> documents) {
        List<Document> docs = documents.stream().map(doc -> {
            Document d = new Document();
            d.setTitle(doc.get("title"));
            d.setContent(doc.get("content"));
            d.setCategory(doc.getOrDefault("category", "general"));
            return d;
        }).toList();

        return documentRepository.saveAll(docs);
    }
}