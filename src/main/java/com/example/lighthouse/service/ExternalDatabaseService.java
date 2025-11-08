// src/main/java/com/example/lighthouse/service/ExternalDatabaseService.java
package com.example.lighthouse.service;

import com.example.lighthouse.Model.DatabaseConnection;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class ExternalDatabaseService {

    // Test connection
    public boolean testConnection(DatabaseConnection dbConfig) {
        String url = buildJdbcUrl(dbConfig);
        try (Connection conn = DriverManager.getConnection(
                url,
                dbConfig.getUsername(),
                dbConfig.getPassword()
        )) {
            return conn.isValid(5);
        } catch (SQLException e) {
            System.err.println("Connection test failed: " + e.getMessage());
            return false;
        }
    }

    // Get all tables from the database
    public List<String> getTables(DatabaseConnection dbConfig) throws SQLException {
        String url = buildJdbcUrl(dbConfig);
        List<String> tables = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, dbConfig.getUsername(), dbConfig.getPassword())) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"});

            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }

        return tables;
    }

    // Query the database (for RAG context)
    public List<Map<String, Object>> queryDatabase(DatabaseConnection dbConfig, String query) throws SQLException {
        String url = buildJdbcUrl(dbConfig);
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url, dbConfig.getUsername(), dbConfig.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                results.add(row);
            }
        }

        return results;
    }

    // IMPROVED: Search database for relevant data using SQL LIKE queries
    public String searchDatabase(DatabaseConnection dbConfig, String searchTerm) {
        try {
            String url = buildJdbcUrl(dbConfig);
            List<String> tables = getTables(dbConfig);
            StringBuilder context = new StringBuilder();
            context.append("Relevant data from database:\n\n");

            boolean foundData = false;

            // Split search term into keywords
            String[] keywords = searchTerm.trim().split("\\s+");

            for (String table : tables) {
                List<String> textColumns = new ArrayList<>(); // Declare outside try block

                try (Connection conn = DriverManager.getConnection(url, dbConfig.getUsername(), dbConfig.getPassword())) {
                    // Get column names for this table
                    DatabaseMetaData metaData = conn.getMetaData();
                    ResultSet columns = metaData.getColumns(null, null, table, null);
                    List<String> columnNames = new ArrayList<>();

                    while (columns.next()) {
                        String columnName = columns.getString("COLUMN_NAME");
                        String columnType = columns.getString("TYPE_NAME").toLowerCase();
                        columnNames.add(columnName);

                        // Identify text-like columns (varchar, text, char, etc.)
                        if (columnType.contains("varchar") || columnType.contains("text") ||
                                columnType.contains("char") || columnType.contains("string")) {
                            textColumns.add(columnName);
                        }
                    }

                    if (textColumns.isEmpty()) {
                        continue; // Skip tables with no text columns
                    }

                    // Build WHERE clause with OR conditions for each keyword and column
                    StringBuilder whereClause = new StringBuilder();
                    for (int i = 0; i < keywords.length; i++) {
                        String keyword = keywords[i].trim();
                        if (keyword.isEmpty()) continue;

                        if (i > 0) whereClause.append(" OR ");

                        whereClause.append("(");
                        for (int j = 0; j < textColumns.size(); j++) {
                            if (j > 0) whereClause.append(" OR ");
                            whereClause.append(String.format("LOWER(CAST(%s AS TEXT)) LIKE LOWER(?)",
                                    textColumns.get(j)));
                        }
                        whereClause.append(")");
                    }

                    // Execute query with LIMIT to avoid too much data
                    String query = String.format(
                            "SELECT * FROM %s WHERE %s LIMIT 50",
                            table,
                            whereClause.toString()
                    );

                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        // Set parameters for each keyword
                        int paramIndex = 1;
                        for (String keyword : keywords) {
                            if (keyword.isEmpty()) continue;
                            for (String col : textColumns) {
                                pstmt.setString(paramIndex++, "%" + keyword + "%");
                            }
                        }

                        ResultSet rs = pstmt.executeQuery();
                        ResultSetMetaData rsMeta = rs.getMetaData();
                        int columnCount = rsMeta.getColumnCount();

                        int rowCount = 0;
                        while (rs.next() && rowCount < 20) { // Limit to 20 rows per table
                            foundData = true;
                            context.append("Table: ").append(table).append("\n");
                            context.append("Row ").append(rowCount + 1).append(":\n");

                            for (int i = 1; i <= columnCount; i++) {
                                String columnName = rsMeta.getColumnName(i);
                                Object value = rs.getObject(i);
                                if (value != null) {
                                    context.append("  ").append(columnName).append(": ").append(value.toString()).append("\n");
                                }
                            }
                            context.append("\n");
                            rowCount++;
                        }
                    }
                } catch (SQLException e) {
                    // If prepared statement fails, try simpler approach
                    System.err.println("Error querying table " + table + ": " + e.getMessage());

                    // Fallback: Simple LIKE query on first text column (if available)
                    if (!textColumns.isEmpty()) {
                        try (Connection conn2 = DriverManager.getConnection(url, dbConfig.getUsername(), dbConfig.getPassword())) {
                            String simpleQuery = String.format(
                                    "SELECT * FROM %s WHERE LOWER(CAST(%s AS TEXT)) LIKE LOWER('%%%s%%') LIMIT 20",
                                    table, textColumns.get(0), searchTerm.replace("'", "''")
                            );

                            try (Statement stmt = conn2.createStatement();
                                 ResultSet rs = stmt.executeQuery(simpleQuery)) {

                                ResultSetMetaData rsMeta = rs.getMetaData();
                                int columnCount = rsMeta.getColumnCount();

                                int rowCount = 0;
                                while (rs.next() && rowCount < 20) {
                                    foundData = true;
                                    context.append("Table: ").append(table).append("\n");
                                    context.append("Row ").append(rowCount + 1).append(":\n");

                                    for (int i = 1; i <= columnCount; i++) {
                                        String columnName = rsMeta.getColumnName(i);
                                        Object value = rs.getObject(i);
                                        if (value != null) {
                                            context.append("  ").append(columnName).append(": ").append(value.toString()).append("\n");
                                        }
                                    }
                                    context.append("\n");
                                    rowCount++;
                                }
                            }
                        } catch (SQLException e2) {
                            System.err.println("Fallback query also failed for " + table + ": " + e2.getMessage());
                        }
                    }
                }
            }

            if (!foundData) {
                context.append("No matching data found in database for: ").append(searchTerm).append("\n");
                context.append("Please check if the data exists or try a different search term.\n");
            }

            return context.toString();

        } catch (Exception e) {
            return "Error searching database: " + e.getMessage() + "\nStack trace: " +
                    Arrays.toString(e.getStackTrace());
        }
    }

    // Get table schema (for AI to understand structure)
    public String getTableSchema(DatabaseConnection dbConfig, String tableName) throws SQLException {
        String url = buildJdbcUrl(dbConfig);
        StringBuilder schema = new StringBuilder();

        try (Connection conn = DriverManager.getConnection(url, dbConfig.getUsername(), dbConfig.getPassword())) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, tableName, null);

            schema.append("Table: ").append(tableName).append("\nColumns:\n");
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                schema.append("  - ").append(columnName).append(" (").append(columnType).append(")\n");
            }
        }

        return schema.toString();
    }

    private String buildJdbcUrl(DatabaseConnection dbConfig) {
        return String.format("jdbc:postgresql://%s:%d/%s",
                dbConfig.getHost(),
                dbConfig.getPort(),
                dbConfig.getDatabase()
        );
    }
}