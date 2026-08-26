package com.grash.advancedsearch;

import com.grash.model.ApiKey;
import com.grash.model.User;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public final class SearchFieldPolicy {

    public static final Set<String> SENSITIVE_PROPERTIES = Set.of(
            "password",
            "secret",
            "token"
    );

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
     * The per-entity blocklist is enforced at every level of the path, not
     * just the root: each segment's declared Java type is resolved via
     * reflection (unwrapping Collection/Map element types), so a rule
     * defined for {@code User} also applies to e.g. {@code primaryUser.*}
     * when the root entity has a {@code User}-typed relation.
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
        String[] segments = field.split("\\.");
        Class<?> currentClass = entityClass;

        for (int i = 0; i < segments.length && currentClass != null; i++) {
            String remainder = String.join(".", Arrays.copyOfRange(segments, i, segments.length));
            Set<String> disallowed = DISALLOWED_FIELDS.get(currentClass);
            if (disallowed != null) {
                for (String path : disallowed) {
                    if (remainder.equals(path) || remainder.startsWith(path + ".")) {
                        return true;
                    }
                }
            }
            currentClass = resolveFieldType(currentClass, segments[i]);
        }
        return false;
    }

    /**
     * Resolves the Java type a field name points to, walking up the
     * superclass chain, and unwrapping Collection/Map generics to their
     * element/value type so relation lists (e.g. {@code List<User>}) are
     * checked against the target entity's rules too.
     */
    private static Class<?> resolveFieldType(Class<?> owner, String fieldName) {
        Class<?> current = owner;
        while (current != null && current != Object.class) {
            try {
                return unwrapType(current.getDeclaredField(fieldName));
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Class<?> unwrapType(Field f) {
        Class<?> type = f.getType();
        if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
            Type generic = f.getGenericType();
            if (generic instanceof ParameterizedType pt) {
                Type[] args = pt.getActualTypeArguments();
                Type target = args[args.length - 1]; // element type for Collection, value type for Map
                if (target instanceof Class<?> cls) {
                    return cls;
                }
            }
            return null;
        }
        return type;
    }
}