package com.grash.automation.eval;

import com.grash.automation.event.EntityChangedEvent;
import com.grash.model.Company;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Everything a condition or an action gets to see during one run.
 *
 * <p>The operand cache is not an optimisation to be added later. {@code enable_lazy_load_no_trans}
 * is on, so every unresolved association a resolver touches opens its own session and issues its
 * own query, silently. A rule with four conditions on the same asset would otherwise read that
 * asset four times.
 */
@Getter
@RequiredArgsConstructor
public class ExecutionContext {

    private final EntityChangedEvent event;
    private final Company company;

    /**
     * The entity that triggered the rule, loaded fresh after the commit. Typed as Object because
     * the engine is generic over entity types; resolvers and handlers know what they asked for.
     */
    private final Object triggerEntity;

    private final Map<String, Object> operandCache = new HashMap<>();

    /**
     * Caches null as a result too. {@code computeIfAbsent} would not: an asset without a value
     * for a custom field is the normal case, and it would be reloaded for every condition that
     * asks about it.
     */
    public Object cached(String subject, java.util.function.Supplier<Object> loader) {
        if (operandCache.containsKey(subject)) {
            return operandCache.get(subject);
        }
        Object value = loader.get();
        operandCache.put(subject, value);
        return value;
    }
}
