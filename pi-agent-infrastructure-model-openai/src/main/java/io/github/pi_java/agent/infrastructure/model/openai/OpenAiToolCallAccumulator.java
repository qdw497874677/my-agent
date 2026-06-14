package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.model.ProviderErrorSummary;
import io.github.pi_java.agent.domain.tool.ToolCall;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class OpenAiToolCallAccumulator {
    private final RunId runId;
    private final StepId stepId;
    private final Map<Integer, Builder> builders = new LinkedHashMap<>();

    public OpenAiToolCallAccumulator(RunId runId, StepId stepId) {
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.stepId = Objects.requireNonNull(stepId, "stepId must not be null");
    }

    public Optional<ToolCall> add(Fragment fragment) {
        Objects.requireNonNull(fragment, "fragment must not be null");
        Builder builder = builders.computeIfAbsent(fragment.index(), ignored -> new Builder());
        builder.append(fragment);
        Optional<Map<String, Object>> arguments = parseCompleteObject(builder.arguments.toString());
        if (arguments.isPresent() && builder.hasName()) {
            builders.remove(fragment.index());
            return Optional.of(new ToolCall(builder.toolCallId(), runId, stepId, builder.name, arguments.orElseThrow(), Instant.now()));
        }
        return Optional.empty();
    }

    public Optional<ProviderErrorSummary> validateComplete() {
        for (Builder builder : builders.values()) {
            if (builder.arguments.isEmpty()) {
                return Optional.of(OpenAiProviderErrorMapper.incompleteToolCallArguments("Tool call arguments were not provided"));
            }
            String raw = builder.arguments.toString();
            if (looksPotentiallyIncomplete(raw)) {
                return Optional.of(OpenAiProviderErrorMapper.incompleteToolCallArguments("Tool call arguments JSON is incomplete"));
            }
            if (parseCompleteObject(raw).isEmpty()) {
                return Optional.of(OpenAiProviderErrorMapper.invalidToolCallArguments("Tool call arguments are not valid JSON object payload"));
            }
        }
        return Optional.empty();
    }

    private Optional<Map<String, Object>> parseCompleteObject(String raw) {
        if (raw == null || raw.isBlank() || looksPotentiallyIncomplete(raw)) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return Optional.empty();
        }
        try {
            return Optional.of(SimpleJsonObjectParser.parse(trimmed));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static boolean looksPotentiallyIncomplete(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                inString = !inString;
            } else if (!inString && c == '{') {
                depth++;
            } else if (!inString && c == '}') {
                depth--;
            }
        }
        return inString || depth > 0 || trimmed.endsWith(":") || trimmed.endsWith(",");
    }

    public record Fragment(int index, String id, String name, String argumentsDelta) {
        public Fragment {
            if (index < 0) {
                throw new IllegalArgumentException("index must not be negative");
            }
        }
    }

    private static final class Builder {
        private String id;
        private String name;
        private final StringBuilder arguments = new StringBuilder();

        void append(Fragment fragment) {
            if (fragment.id() != null && !fragment.id().isBlank()) {
                id = fragment.id();
            }
            if (fragment.name() != null && !fragment.name().isBlank()) {
                name = fragment.name();
            }
            if (fragment.argumentsDelta() != null) {
                arguments.append(fragment.argumentsDelta());
            }
        }

        boolean hasName() {
            return name != null && !name.isBlank();
        }

        String toolCallId() {
            return id == null || id.isBlank() ? "tool-call-" + Math.abs(Objects.hash(name, arguments.toString())) : id;
        }
    }

    private static final class SimpleJsonObjectParser {
        private final String json;
        private int index;

        private SimpleJsonObjectParser(String json) {
            this.json = json;
        }

        static Map<String, Object> parse(String json) {
            SimpleJsonObjectParser parser = new SimpleJsonObjectParser(json);
            Map<String, Object> parsed = parser.object();
            parser.skipWhitespace();
            if (parser.index != parser.json.length()) {
                throw new IllegalArgumentException("unexpected trailing content");
            }
            return parsed;
        }

        private Map<String, Object> object() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return result;
            }
            while (true) {
                String key = string();
                skipWhitespace();
                expect(':');
                Object value = value();
                result.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return result;
                }
                expect(',');
            }
        }

        private Object value() {
            skipWhitespace();
            if (peek('"')) {
                return string();
            }
            if (peek('{')) {
                return object();
            }
            if (consume("true")) {
                return true;
            }
            if (consume("false")) {
                return false;
            }
            if (consume("null")) {
                return null;
            }
            return number();
        }

        private Number number() {
            int start = index;
            while (index < json.length() && "-0123456789.eE+".indexOf(json.charAt(index)) >= 0) {
                index++;
            }
            if (start == index) {
                throw new IllegalArgumentException("expected value");
            }
            String raw = json.substring(start, index);
            return raw.contains(".") || raw.contains("e") || raw.contains("E") ? Double.parseDouble(raw) : Long.parseLong(raw);
        }

        private String string() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < json.length()) {
                char c = json.charAt(index++);
                if (c == '"') {
                    return builder.toString();
                }
                if (c == '\\') {
                    if (index >= json.length()) {
                        throw new IllegalArgumentException("invalid escape");
                    }
                    char escaped = json.charAt(index++);
                    builder.append(escaped == 'n' ? '\n' : escaped == 't' ? '\t' : escaped);
                } else {
                    builder.append(c);
                }
            }
            throw new IllegalArgumentException("unterminated string");
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= json.length() || json.charAt(index) != expected) {
                throw new IllegalArgumentException("expected " + expected);
            }
            index++;
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return index < json.length() && json.charAt(index) == expected;
        }

        private boolean consume(String expected) {
            skipWhitespace();
            if (json.startsWith(expected, index)) {
                index += expected.length();
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }
    }
}
