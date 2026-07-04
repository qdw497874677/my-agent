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

            // Phase 16 Plan 03 (D-17): additive column/index alignment so the
            // local SQLite profile can serve the same conversation read-model
            // ports as the JDBC layer. Columns are guarded by missing-column
            // checks so existing databases upgrade in place; tables already
            // created above with CREATE TABLE IF NOT EXISTS.
            addColumnIfMissing(conn, "local_runs", "input_json", "TEXT");
            addColumnIfMissing(conn, "local_runs", "trace_id", "TEXT");
            addColumnIfMissing(conn, "local_runs", "correlation_id", "TEXT");
            addColumnIfMissing(conn, "local_runs", "provider_metadata_json", "TEXT");
            addColumnIfMissing(conn, "local_events", "session_id", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(conn, "local_events", "tenant_id", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(conn, "local_events", "user_id", "TEXT NOT NULL DEFAULT ''");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_local_sessions_user_updated ON local_sessions(user_id, updated_at)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_local_runs_session_created ON local_runs(session_id, created_at)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_local_events_run_sequence ON local_events(run_id, sequence)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_local_events_owner_run ON local_events(tenant_id, user_id, session_id, run_id)");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init SQLite schema: " + e.getMessage(), e);
        }
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String definition) throws SQLException {
        boolean exists = false;
        try (PreparedStatement ps = conn.prepareStatement("PRAGMA table_info(" + table + ")");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
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
             ResultSet rs = stmt.executeQuery("SELECT * FROM local_sessions ORDER BY updated_at DESC")) {
            while (rs.next()) {
                result.add(rowToMap(rs));
            }
        } catch (SQLException e) {
            // table may not exist yet
        }
        return result;
    }

    /**
     * Phase 16 Plan 03 (D-17): recent-session load ordered by latest activity
     * and filtered by tenant/user at the SQL layer (D-15), mirroring the JDBC
     * {@code SessionRepository.listRecent} contract for the local profile.
     */
    public List<Map<String, String>> loadRecentSessions(String tenantId, String userId, int limit) {
        List<Map<String, String>> result = new ArrayList<>();
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM local_sessions WHERE tenant_id = ? AND user_id = ? ORDER BY updated_at DESC LIMIT ?")) {
            ps.setString(1, tenantId);
            ps.setString(2, userId);
            ps.setInt(3, limit > 0 ? limit : 20);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            // table may not exist yet
        }
        return result;
    }

    // === Runs ===

    public void saveRun(String runId, String sessionId, String tenantId, String userId, String workspaceId,
                        String status, String createdAt, String updatedAt, String resultJson, String failureJson) {
        saveRun(runId, sessionId, tenantId, userId, workspaceId, status, createdAt, updatedAt, resultJson, failureJson, null, null, null);
    }

    /**
     * Phase 16 Plan 03 (D-17): richer save that retains the run input JSON and
     * trace/correlation fields needed for transcript USER messages, mirroring
     * the JDBC {@code runs.input} column.
     */
    public void saveRun(String runId, String sessionId, String tenantId, String userId, String workspaceId,
                        String status, String createdAt, String updatedAt, String resultJson, String failureJson,
                        String inputJson, String traceId, String correlationId) {
        saveRun(runId, sessionId, tenantId, userId, workspaceId, status, createdAt, updatedAt, resultJson, failureJson,
                inputJson, traceId, correlationId, null);
    }

    /**
     * Phase 20 Plan 03: stores safe provider/model/fallback metadata in the
     * local profile using additive SQLite schema. The JSON must already be
     * redacted and limited to safe run facts; raw provider config and secrets do
     * not belong in this column.
     */
    public void saveRun(String runId, String sessionId, String tenantId, String userId, String workspaceId,
                        String status, String createdAt, String updatedAt, String resultJson, String failureJson,
                        String inputJson, String traceId, String correlationId, String providerMetadataJson) {
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(
                      "INSERT OR REPLACE INTO local_runs (run_id, session_id, tenant_id, user_id, workspace_id, " +
                              "status, created_at, updated_at, result_json, failure_json, input_json, trace_id, correlation_id, " +
                              "provider_metadata_json) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
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
            ps.setString(11, inputJson);
            ps.setString(12, traceId);
            ps.setString(13, correlationId);
            ps.setString(14, providerMetadataJson);
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

    /**
     * Phase 16 Plan 03 (D-17): session-owned run load filtered by tenant/user
     * at the SQL layer (D-15) and ordered by creation ascending for stable
     * transcript assembly (D-09, D-16), mirroring the JDBC
     * {@code RunProjectionRepository.listRunsBySession} contract. Retains the
     * input JSON needed for USER transcript messages.
     */
    public List<Map<String, String>> loadRunsBySession(String tenantId, String userId, String sessionId, int limit) {
        List<Map<String, String>> result = new ArrayList<>();
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM local_runs WHERE tenant_id = ? AND user_id = ? AND session_id = ? " +
                             "ORDER BY created_at ASC LIMIT ?")) {
            ps.setString(1, tenantId);
            ps.setString(2, userId);
            ps.setString(3, sessionId);
            ps.setInt(4, limit > 0 ? limit : 20);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            // table may not exist yet
        }
        return result;
    }

    // === Events ===

    public void appendEvent(String eventId, String runId, long sequence, String eventType,
                            String timestamp, String payloadJson) {
        appendEvent(eventId, runId, "", "", "", sequence, eventType, timestamp, payloadJson);
    }

    /**
     * Phase 16 Plan 03 (D-17): richer append that records the ownership columns
     * (tenant/user/session) so events can be loaded by session/run with the
     * same ownership filters as JDBC (D-15).
     */
    public void appendEvent(String eventId, String runId, String tenantId, String userId, String sessionId,
                            long sequence, String eventType, String timestamp, String payloadJson) {
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO local_events (event_id, run_id, tenant_id, user_id, session_id, " +
                             "sequence, event_type, timestamp, payload_json) VALUES (?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, eventId);
            ps.setString(2, runId);
            ps.setString(3, tenantId);
            ps.setString(4, userId);
            ps.setString(5, sessionId);
            ps.setLong(6, sequence);
            ps.setString(7, eventType);
            ps.setString(8, timestamp);
            ps.setString(9, payloadJson);
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

    public List<Map<String, String>> loadEventsByRun(String runId, long afterSequence, int limit) {
        List<Map<String, String>> result = new ArrayList<>();
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM local_events WHERE run_id = ? AND sequence > ? ORDER BY sequence ASC LIMIT ?")) {
            ps.setString(1, runId);
            ps.setLong(2, afterSequence);
            ps.setInt(3, limit > 0 ? limit : 100);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            // table may not exist yet
        }
        return result;
    }

    /**
     * Phase 16 Plan 03 (D-17): ownership-safe event read path filtered by
     * tenant/user/session/run at the SQL layer (D-05, D-15), mirroring the JDBC
     * {@code RunEventStore.listBySessionRun} contract. Events arrive in
     * ascending sequence order.
     */
    public List<Map<String, String>> loadEventsBySessionRun(String tenantId, String userId, String sessionId,
                                                            String runId, long afterSequence, int limit) {
        List<Map<String, String>> result = new ArrayList<>();
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM local_events WHERE tenant_id = ? AND user_id = ? AND session_id = ? " +
                             "AND run_id = ? AND sequence > ? ORDER BY sequence ASC LIMIT ?")) {
            ps.setString(1, tenantId);
            ps.setString(2, userId);
            ps.setString(3, sessionId);
            ps.setString(4, runId);
            ps.setLong(5, afterSequence);
            ps.setInt(6, limit > 0 ? limit : 100);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
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
