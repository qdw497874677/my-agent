package io.github.pi_java.agent.infrastructure.jdbc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

final class JdbcJson {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private JdbcJson() {
    }

    static PGobject jsonb(Map<String, Object> value) {
        try {
            PGobject object = new PGobject();
            object.setType("jsonb");
            object.setValue(OBJECT_MAPPER.writeValueAsString(value == null ? Map.of() : value));
            return object;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to serialize JSONB value", ex);
        }
    }

    static PGobject jsonb(Object value) {
        try {
            PGobject object = new PGobject();
            object.setType("jsonb");
            object.setValue(OBJECT_MAPPER.writeValueAsString(value));
            return object;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to serialize JSONB value", ex);
        }
    }

    static Map<String, Object> readMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        try {
            String json = value instanceof PGobject pg ? pg.getValue() : value.toString();
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return OBJECT_MAPPER.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to read JSONB map", ex);
        }
    }

    static PGobject redaction(boolean containsSecrets, boolean redacted, Set<String> redactedFields, String policyRef) {
        return jsonb(Map.of(
                "containsSecrets", containsSecrets,
                "redacted", redacted,
                "redactedFields", redactedFields == null ? Set.of() : redactedFields,
                "policyRef", policyRef == null ? "" : policyRef));
    }
}
