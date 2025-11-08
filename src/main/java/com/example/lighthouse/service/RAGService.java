// src/main/java/com/example/lighthouse/service/RAGService.java
package com.example.lighthouse.service;

import com.example.lighthouse.Model.Document;
import com.example.lighthouse.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RAGService {

    @Autowired
    private DocumentRepository documentRepository;

    // Find relevant documents for a query
    public List<Document> findRelevantDocuments(String query, int maxResults) {
        // Extract keywords from query (simple approach)
        String[] keywords = query.toLowerCase().split("\\s+");

        // Search for documents containing any keyword
        List<Document> allMatches = documentRepository.searchByKeyword(keywords[0]);

        // Limit results
        return allMatches.stream()
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    // Build context from documents
    public String buildContext(List<Document> documents) {
        if (documents.isEmpty()) {
            return "No relevant documents found.";
        }

        StringBuilder context = new StringBuilder();
        context.append("Here is relevant information from our knowledge base:\n\n");

        for (Document doc : documents) {
            context.append("--- ").append(doc.getTitle()).append(" ---\n");
            context.append(doc.getContent()).append("\n\n");
        }

        return context.toString();
    }
}