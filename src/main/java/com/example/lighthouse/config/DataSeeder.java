// src/main/java/com/example/lighthouse/config/DataSeeder.java
package com.example.lighthouse.config;

import com.example.lighthouse.Model.Document;
import com.example.lighthouse.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private DocumentRepository documentRepository;

    @Override
    public void run(String... args) throws Exception {
        // Only seed if database is empty
        if (documentRepository.count() == 0) {
            seedSampleDocuments();
        }
    }

    private void seedSampleDocuments() {
        // Sample company knowledge base
        Document doc1 = new Document();
        doc1.setTitle("Company Overview");
        doc1.setContent("TechCorp is a leading AI solutions provider founded in 2020. We specialize in enterprise AI monitoring and cost optimization tools. Our flagship product, AI Observatory, helps companies track and optimize their AI spending in real-time.");
        doc1.setCategory("about");

        Document doc2 = new Document();
        doc2.setTitle("Pricing Plans");
        doc2.setContent("We offer three pricing tiers: Free (1,000 traces/month), Pro ($29/month with 50,000 traces), and Enterprise ($299/month with unlimited traces). All plans include real-time monitoring, cost tracking, and basic analytics.");
        doc2.setCategory("pricing");

        Document doc3 = new Document();
        doc3.setTitle("Supported AI Providers");
        doc3.setContent("AI Observatory currently supports OpenAI (GPT-3.5, GPT-4), Google Gemini (2.0 Flash, Pro), and Anthropic Claude (Sonnet, Opus). We're working on adding support for Azure OpenAI and AWS Bedrock in Q2 2025.");
        doc3.setCategory("features");

        Document doc4 = new Document();
        doc4.setTitle("Getting Started Guide");
        doc4.setContent("To get started: 1) Sign up for an account, 2) Connect your AI provider API key in Settings, 3) Install our SDK with Maven or Gradle, 4) Start making AI calls through our tracer. Your first trace will appear in the dashboard within seconds!");
        doc4.setCategory("docs");

        Document doc5 = new Document();
        doc5.setTitle("Cost Optimization Tips");
        doc5.setContent("To reduce AI costs: Use GPT-3.5 instead of GPT-4 when possible (10x cheaper), implement caching for repeated queries, set token limits on responses, and use our cost alerts to catch expensive queries early. Most customers reduce costs by 40% in the first month.");
        doc5.setCategory("tips");

        documentRepository.save(doc1);
        documentRepository.save(doc2);
        documentRepository.save(doc3);
        documentRepository.save(doc4);
        documentRepository.save(doc5);

        System.out.println("✅ Seeded 5 sample documents");
    }
}