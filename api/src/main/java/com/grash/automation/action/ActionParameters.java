package com.grash.automation.action;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.automation.eval.ExecutionContext;
import com.grash.exception.CustomException;
import com.grash.model.Asset;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An action's JSON parameters, with {@code ${trigger.…}} placeholders already resolved.
 *
 * <p>This is the piece the old engine is missing. Its actions could only carry references fixed
 * at configuration time, so "create a work order for <em>this</em> asset" was inexpressible — the
 * reason the AI triage bypasses the engine entirely rather than using it to apply what it
 * decided.
 *
 * <p>It is a <b>closed</b> mechanism, not an expression language: {@link #PLACEHOLDERS} is the
 * whole vocabulary. An unknown placeholder is an error, not an empty string, because a rule that
 * quietly inserts nothing where an asset id should go is worse than one that refuses to run.
 */
public final class ActionParameters {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    /** The complete list of what a rule author may reference. */
    public static final Map<String, java.util.function.Function<ExecutionContext, Object>> PLACEHOLDERS = Map.of(
            "trigger.id", context -> context.getEvent().entityId(),
            "trigger.asset.id", context -> asset(context).getId(),
            "trigger.asset.name", context -> asset(context).getName(),
            "trigger.asset.status", context -> asset(context).getStatus() == null
                    ? null : asset(context).getStatus().name()
    );

    private final Map<String, Object> values;

    private ActionParameters(Map<String, Object> values) {
        this.values = values;
    }

    public static ActionParameters of(String json, ExecutionContext context) {
        if (json == null || json.isBlank()) {
            return new ActionParameters(Map.of());
        }
        Map<String, Object> raw;
        try {
            raw = MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new CustomException("Action parameters are not valid JSON: " + exception.getMessage(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        raw.replaceAll((key, value) -> value instanceof String text ? substitute(text, context) : value);
        return new ActionParameters(raw);
    }

    /**
     * A placeholder that is the entire value keeps its type — {@code "${trigger.asset.id}"}
     * yields a number, not the string "42", so a reference does not have to be parsed back.
     * Inside a longer string it is interpolated as text, which is what a title wants.
     */
    private static Object substitute(String text, ExecutionContext context) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        if (matcher.matches()) {
            return resolve(matcher.group(1), context);
        }
        return matcher.replaceAll(match -> {
            Object resolved = resolve(match.group(1), context);
            return Matcher.quoteReplacement(resolved == null ? "" : String.valueOf(resolved));
        });
    }

    private static Object resolve(String name, ExecutionContext context) {
        var placeholder = PLACEHOLDERS.get(name.trim());
        if (placeholder == null) {
            throw new CustomException("Unknown placeholder ${" + name + "}. Known: "
                    + PLACEHOLDERS.keySet(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return placeholder.apply(context);
    }

    private static Asset asset(ExecutionContext context) {
        if (context.getTriggerEntity() instanceof Asset asset) {
            return asset;
        }
        throw new CustomException("This rule was not triggered by an asset, so ${trigger.asset.…} "
                + "cannot be resolved", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public String getString(String key) {
        Object value = values.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public String requireString(String key) {
        String value = getString(key);
        if (value == null || value.isBlank()) {
            throw new CustomException("Action parameter \"" + key + "\" is required",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return value;
    }

    public Long getLong(String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            throw new CustomException("Action parameter \"" + key + "\" must be a number, was \""
                    + value + "\"", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public Long requireLong(String key) {
        Long value = getLong(key);
        if (value == null) {
            throw new CustomException("Action parameter \"" + key + "\" is required",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return value;
    }
}
