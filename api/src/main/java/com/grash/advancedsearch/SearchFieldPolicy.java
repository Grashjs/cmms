package com.grash.advancedsearch;

import com.grash.model.ApiKey;
import com.grash.model.User;

import java.util.Map;
import java.util.Set;

/**
 * Central, DRY source of truth for which properties may NOT be used in
 * advanced searches.
 * <p>
 * Every {@link WrapperSpecification} validates the field path it receives
 * against this policy before any JPA query is built. This prevents clients
 * from probing internal or sensitive properties (e.g. {@code password},
 * {@code createdBy.password}, API keys, tokens, ...) through the search
 * engine.
 * <p>
 * Validation rules:
 * <ul>
 *     <li>Names in {@link #SENSITIVE_PROPERTIES} are never searchable, at any
 *     depth of a dotted path, for any entity.</li>
 *     <li>Paths listed in {@link #DISALLOWED_FIELDS} for an entity are never
 *     searchable on that entity. The check is applied to the full dotted path
 *     and to every prefix, so disallowing {@code createdBy} also rejects
 *     {@code createdBy.password}, {@code createdBy.internalField}, etc.</li>
 * </ul>
 */
public final class SearchFieldPolicy {

    /**
     * Property names that must never be used as search criteria, regardless of
     * the root entity or the depth of the path. Applied to every segment of a
     * dotted path.
     */
    public static final Set<String> SENSITIVE_PROPERTIES = Set.of(
            "password",
            "secret",
            "token"
    );

    /**
     * Dotted paths that are explicitly forbidden for search on a given root
     * entity. Each entry is matched against the full path and against every
     * prefix of it.
     */
    private static final Map<Class<?>, Set<String>> DISALLOWED_FIELDS = Map.ofEntries(
            Map.entry(User.class, Set.of(
                    "userSettings",
                    "appStats",
                    "superAccountRelations",
                    "parentSuperAccount"
            )),
            Map.entry(ApiKey.class, Set.of("code"))
    );

    /**
     * Validates a field path against the policy for the given root entity.
     *
     * @param entityClass the JPA entity being searched; may be null when the
     *                    type is not resolvable (e.g. in unit tests), in which
     *                    case only the global blocklist is enforced
     * @param field       the dotted field path from the client
     * @throws InvalidSearchFieldException if the path is not allowed
     */
    public static void validate(Class<?> entityClass, String field) {
        if (field == null || field.isBlank()) {
            throw new InvalidSearchFieldException(field);
        }
        for (String segment : field.split("\\.")) {
            if (SENSITIVE_PROPERTIES.contains(segment)) {
                throw new InvalidSearchFieldException(field);
            }
        }
        if (isDisallowed(entityClass, field)) {
            throw new InvalidSearchFieldException(field);
        }
    }

    private static boolean isDisallowed(Class<?> entityClass, String field) {
        if (entityClass == null) {
            return false;
        }
        Set<String> disallowed = DISALLOWED_FIELDS.get(entityClass);
        if (disallowed == null || disallowed.isEmpty()) {
            return false;
        }
        for (String path : disallowed) {
            if (field.equals(path) || field.startsWith(path + ".")) {
                return true;
            }
        }
        return false;
    }
}
