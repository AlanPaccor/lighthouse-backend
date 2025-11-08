// src/main/java/com/example/lighthouse/Model/DatabaseConnection.java
package com.example.lighthouse.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "database_connections")
public class DatabaseConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name; // User-friendly name: "Production DB", "Sales Data"
    private String host; // e.g., "localhost" or "db.example.com"
    private Integer port; // e.g., 5432
    private String database; // Database name
    private String username;
    private String password; // Should be encrypted in production

    private Boolean isConnected = false;
    private String lastError;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getIsConnected() { return isConnected; }
    public void setIsConnected(Boolean isConnected) { this.isConnected = isConnected; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}