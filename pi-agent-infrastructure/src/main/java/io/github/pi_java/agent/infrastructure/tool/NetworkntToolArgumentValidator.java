package io.github.pi_java.agent.infrastructure.tool;

import com.networknt.schema.Error;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.serialization.JsonMapperFactory;
import io.github.pi_java.agent.app.port.tool.ToolArgumentValidator;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Infrastructure-only JSON Schema validator backed by networknt.
 */
public final class NetworkntToolArgumentValidator implements ToolArgumentValidator {

    private static final int MAX_MESSAGES = 5;

    @Override
    public ValidationResult validate(ToolDescriptor descriptor, ToolExecutionRequest request) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(request, "request must not be null");
        try {
            SchemaRegistry registry = SchemaRegistry.withDefaultDialect(specVersion(descriptor.inputSchema().dialect()));
            Schema schema = registry.getSchema(
                    SchemaLocation.of("urn:pi-tool-schema:" + descriptor.id() + ":input"),
                    JsonMapperFactory.getInstance().valueToTree(descriptor.inputSchema().document())
            );
            List<Error> errors = schema.validate(JsonMapperFactory.getInstance().valueToTree(request.arguments()));
            if (errors.isEmpty()) {
                return ValidationResult.ok();
            }
            List<String> safeMessages = errors.stream()
                    .sorted(Comparator.comparing((Error error) -> String.valueOf(error.getEvaluationPath()))
                            .thenComparing(Error::getKeyword))
                    .limit(MAX_MESSAGES)
                    .map(this::safeMessage)
                    .toList();
            return ValidationResult.invalid("TOOL_ARGUMENT_SCHEMA_INVALID", "tool arguments failed schema validation", Map.of(
                    "schemaDialect", descriptor.inputSchema().dialect(),
                    "errorCount", errors.size(),
                    "errors", safeMessages
            ));
        } catch (RuntimeException ex) {
            return ValidationResult.invalid("TOOL_ARGUMENT_SCHEMA_ERROR", "tool input schema could not be evaluated", Map.of(
                    "schemaDialect", descriptor.inputSchema().dialect(),
                    "errorType", ex.getClass().getSimpleName()
            ));
        }
    }

    private String safeMessage(Error error) {
        String path = error.getEvaluationPath() == null ? "$" : error.getEvaluationPath().toString();
        String keyword = error.getKeyword() == null ? "validation" : error.getKeyword();
        return path + ": " + keyword;
    }

    private SpecificationVersion specVersion(String dialect) {
        String normalized = dialect == null ? "" : dialect.toLowerCase(Locale.ROOT);
        if (normalized.contains("2020-12") || normalized.contains("2020_12")) {
            return SpecificationVersion.DRAFT_2020_12;
        }
        if (normalized.contains("2019-09") || normalized.contains("2019_09")) {
            return SpecificationVersion.DRAFT_2019_09;
        }
        if (normalized.contains("draft-07") || normalized.contains("draft7")) {
            return SpecificationVersion.DRAFT_7;
        }
        if (normalized.contains("draft-06") || normalized.contains("draft6")) {
            return SpecificationVersion.DRAFT_6;
        }
        if (normalized.contains("draft-04") || normalized.contains("draft4")) {
            return SpecificationVersion.DRAFT_4;
        }
        return SpecificationVersion.DRAFT_2020_12;
    }
}
