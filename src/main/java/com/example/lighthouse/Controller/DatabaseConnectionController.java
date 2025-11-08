// src/main/java/com/example/lighthouse/controller/DatabaseConnectionController.java
package com.example.lighthouse.Controller;

import com.example.lighthouse.Model.DatabaseConnection;
import com.example.lighthouse.repository.DatabaseConnectionRepository;
import com.example.lighthouse.service.ExternalDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db-connections")
@CrossOrigin(origins = "http://localhost:5173")
public class DatabaseConnectionController {

    @Autowired
    private DatabaseConnectionRepository dbConnectionRepository;

    @Autowired
    private ExternalDatabaseService externalDbService;

    // Get all connections
    @GetMapping
    public List<DatabaseConnection> getAllConnections() {
        return dbConnectionRepository.findAllByOrderByCreatedAtDesc();
    }

    // Get single connection
    @GetMapping("/{id}")
    public ResponseEntity<DatabaseConnection> getConnection(@PathVariable String id) {
        return dbConnectionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Create new connection
    @PostMapping
    public DatabaseConnection createConnection(@RequestBody DatabaseConnection dbConfig) {
        // Test connection before saving
        boolean isValid = externalDbService.testConnection(dbConfig);
        dbConfig.setIsConnected(isValid);

        if (!isValid) {
            dbConfig.setLastError("Failed to connect. Check credentials.");
        }

        return dbConnectionRepository.save(dbConfig);
    }

    // Test existing connection
    @PostMapping("/{id}/test")
    public Map<String, Object> testConnection(@PathVariable String id) {
        DatabaseConnection dbConfig = dbConnectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        boolean isValid = externalDbService.testConnection(dbConfig);
        dbConfig.setIsConnected(isValid);

        if (isValid) {
            dbConfig.setLastError(null);
        } else {
            dbConfig.setLastError("Connection test failed");
        }

        dbConnectionRepository.save(dbConfig);

        Map<String, Object> response = new HashMap<>();
        response.put("success", isValid);
        response.put("message", isValid ? "Connection successful!" : "Connection failed");
        return response;
    }

    // Get tables from connection
    @GetMapping("/{id}/tables")
    public ResponseEntity<List<String>> getTables(@PathVariable String id) {
        try {
            DatabaseConnection dbConfig = dbConnectionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Connection not found"));

            List<String> tables = externalDbService.getTables(dbConfig);
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // NEW: Get table data (preview rows from a table)
    @GetMapping("/{id}/tables/{tableName}/data")
    public ResponseEntity<Map<String, Object>> getTableData(
            @PathVariable String id,
            @PathVariable String tableName,
            @RequestParam(defaultValue = "100") int limit
    ) {
        try {
            DatabaseConnection dbConfig = dbConnectionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Connection not found"));

            String query = String.format("SELECT * FROM %s LIMIT %d", tableName, limit);
            List<Map<String, Object>> rows = externalDbService.queryDatabase(dbConfig, query);

            Map<String, Object> response = new HashMap<>();
            response.put("tableName", tableName);
            response.put("rowCount", rows.size());
            response.put("data", rows);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // NEW: Get database overview (tables + row counts)
    @GetMapping("/{id}/overview")
    public ResponseEntity<Map<String, Object>> getDatabaseOverview(@PathVariable String id) {
        try {
            DatabaseConnection dbConfig = dbConnectionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Connection not found"));

            List<String> tables = externalDbService.getTables(dbConfig);
            Map<String, Integer> tableRowCounts = new HashMap<>();

            // Get row count for each table
            for (String table : tables) {
                try {
                    String countQuery = String.format("SELECT COUNT(*) as count FROM %s", table);
                    List<Map<String, Object>> result = externalDbService.queryDatabase(dbConfig, countQuery);
                    if (!result.isEmpty()) {
                        Object count = result.get(0).get("count");
                        tableRowCounts.put(table, count instanceof Number ? ((Number) count).intValue() : 0);
                    }
                } catch (Exception e) {
                    tableRowCounts.put(table, -1); // Error getting count
                }
            }

            Map<String, Object> overview = new HashMap<>();
            overview.put("database", dbConfig.getDatabase());
            overview.put("tables", tables);
            overview.put("tableRowCounts", tableRowCounts);
            overview.put("totalTables", tables.size());

            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Search database
    @PostMapping("/{id}/search")
    public Map<String, String> searchDatabase(
            @PathVariable String id,
            @RequestBody Map<String, String> request
    ) {
        DatabaseConnection dbConfig = dbConnectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connection not found"));

        String searchTerm = request.get("query");
        String results = externalDbService.searchDatabase(dbConfig, searchTerm);

        Map<String, String> response = new HashMap<>();
        response.put("results", results);
        return response;
    }

    // Delete connection
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable String id) {
        dbConnectionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}