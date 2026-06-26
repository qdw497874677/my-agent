package io.github.pi_java.agent.adapter.web.provider;

import io.github.pi_java.agent.adapter.web.provider.ProviderConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SqliteLocalPersistence {

    private final String jdbcUrl;

    public SqliteLocalPersistence(String dbPath) {
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
        initSchema();
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void initSchema() {
        try (Connection conn = open(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS local_sessions (
                        session_id TEXT PRIMARY KEY,
                        tenant_id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        workspace_id TEXT NOT NULL,
                        status TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        metadata_json TEXT
                    )""");
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS local_runs (
                        run_id TEXT PRIMARY KEY,
                        session_id TEXT NOT NULL,
                        tenant_id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        workspace_id TEXT NOT NULL,
                        status TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        result_json TEXT,
                        failure_json TEXT
                    )""");
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS local_events (
                        event_id TEXT PRIMARY KEY,
                        run_id TEXT NOT NULL,
                        sequence INTEGER NOT NULL,
                        event_type TEXT NOT NULL,
                        timestamp TEXT NOT NULL,
                        payload_json TEXT
                    )""");
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS local_provider_config (
                        id INTEGER PRIMARY KEY DEFAULT 1,
                        enabled INTEGER NOT NULL DEFAULT 0,
                        base_url TEXT NOT NULL,
                        api_key TEXT NOT NULL DEFAULT '',
                        model_id TEXT NOT NULL,
                        provider_id TEXT NOT NULL,
                        completions_path TEXT NOT NULL
                    )""");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init SQLite schema: " + e.getMessage(), e);
        }
    }

    // === Sessions ===

    public void saveSession(String sessionId, String tenantId, String userId, String workspaceId,
                            String status, String createdAt, String updatedAt, String metadataJson) {
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO local_sessions VALUES (?,?,?,?,?,?,?,?)")) {
            ps.setString(1, sessionId);
            ps.setString(2, tenantId);
            ps.setString(3, userId);
            ps.setString(4, workspaceId);
            ps.setString(5, status);
            ps.setString(6, createdAt);
            ps.setString(7, updatedAt);
            ps.setString(8, metadataJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, String>> loadSessions() {
        List<Map<String, String>> result = new ArrayList<>();
        try (Connection conn = open(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM local_sessions")) {
            while (rs.next()) {
                result.add(rowToMap(rs));
            }
        } catch (SQLException e) {
            // table may not exist yet
        }
        return result;
    }

    // === Runs ===

    public void saveRun(String runId, String sessionId, String tenantId, String userId, String workspaceId,
                        String status, String createdAt, String updatedAt, String resultJson, String failureJson) {
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO local_runs VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, runId);
            ps.setString(2, sessionId);
            ps.setString(3, tenantId);
            ps.setString(4, userId);
            ps.setString(5, workspaceId);
            ps.setString(6, status);
            ps.setString(7, createdAt);
            ps.setString(8, updatedAt);
            ps.setString(9, resultJson);
            ps.setString(10, failureJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, String>> loadRuns() {
        List<Map<String, String>> result = new ArrayList<>();
        try (Connection conn = open(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM local_runs")) {
            while (rs.next()) {
                result.add(rowToMap(rs));
            }
        } catch (SQLException e) {
            // table may not exist yet
        }
        return result;
    }

    // === Events ===

    public void appendEvent(String eventId, String runId, long sequence, String eventType,
                            String timestamp, String payloadJson) {
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO local_events VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, eventId);
            ps.setString(2, runId);
            ps.setLong(3, sequence);
            ps.setString(4, eventType);
            ps.setString(5, timestamp);
            ps.setString(6, payloadJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, String>> loadEvents() {
        List<Map<String, String>> result = new ArrayList<>();
        try (Connection conn = open(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM local_events ORDER BY sequence")) {
            while (rs.next()) {
                result.add(rowToMap(rs));
            }
        } catch (SQLException e) {
            // table may not exist yet
        }
        return result;
    }

    // === Provider Config ===

    public void saveProviderConfig(ProviderConfig config) {
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO local_provider_config (id, enabled, base_url, api_key, model_id, provider_id, completions_path) VALUES (1,?,?,?,?,?,?)")) {
            ps.setInt(1, config.enabled() ? 1 : 0);
            ps.setString(2, config.baseUrl());
            ps.setString(3, config.apiKey());
            ps.setString(4, config.modelId());
            ps.setString(5, config.providerId());
            ps.setString(6, config.completionsPath());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ProviderConfig loadProviderConfig() {
        try (Connection conn = open(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM local_provider_config WHERE id = 1")) {
            if (rs.next()) {
                return new ProviderConfig(
                        rs.getInt("enabled") == 1,
                        rs.getString("base_url"),
                        rs.getString("api_key"),
                        rs.getString("model_id"),
                        rs.getString("provider_id"),
                        rs.getString("completions_path"));
            }
        } catch (SQLException e) {
            // table may not exist yet
        }
        return null;
    }

    private static Map<String, String> rowToMap(ResultSet rs) throws SQLException {
        int cols = rs.getMetaData().getColumnCount();
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 1; i <= cols; i++) {
            map.put(rs.getMetaData().getColumnName(i), rs.getString(i));
        }
        return map;
    }
}
