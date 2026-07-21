package com.grash.advancedsearch;

import com.grash.model.enums.EnumName;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import com.grash.utils.Helper;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class WrapperSpecification<T> implements Specification<T> {

    private final FilterField filterField;
    private final Map<String, Join<?, ?>> joinCache = new HashMap<>();

    public WrapperSpecification(final FilterField filterField) {
        super();
        this.filterField = filterField;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

        Predicate result = null;
        switch (Objects.requireNonNull(SearchOperation.getSimpleOperation(filterField.getOperation()))) {
            case CONTAINS:
                result = buildLikePredicate(root, cb, "%" + filterField.getValue().toString().toLowerCase() + "%");
                break;
            case DOES_NOT_CONTAIN:
                result = cb.not(buildLikePredicate(root, cb, "%" + filterField.getValue().toString().toLowerCase() +
                        "%"));
                break;
            case BEGINS_WITH:
                result = buildLikePredicate(root, cb, filterField.getValue().toString().toLowerCase() + "%");
                break;
            case DOES_NOT_BEGIN_WITH:
                result = cb.not(buildLikePredicate(root, cb, filterField.getValue().toString().toLowerCase() + "%"));
                break;
            case ENDS_WITH:
                result = buildLikePredicate(root, cb, "%" + filterField.getValue().toString().toLowerCase());
                break;
            case DOES_NOT_END_WITH:
                result = cb.not(buildLikePredicate(root, cb, "%" + filterField.getValue().toString().toLowerCase()));
                break;
            case EQUAL: {
                Path<?> path = resolveFieldPath(root, filterField.getField());

                if (path.getJavaType().isAnnotationPresent(jakarta.persistence.Entity.class)) {
                    result = cb.equal(path.get("id"), filterField.getValue());
                } else {
                    result = cb.equal(path, filterField.getValue());
                }
                break;
            }
            case NOT_EQUAL: {
                Path<?> path = resolveFieldPath(root, filterField.getField());

                if (path.getJavaType().isAnnotationPresent(jakarta.persistence.Entity.class)) {
                    result = cb.notEqual(path.get("id"), filterField.getValue());
                } else {
                    result = cb.notEqual(path, filterField.getValue());
                }
                break;
            }
            case NUL:
                result = cb.isNull(resolveFieldPath(root, filterField.getField()));
                break;
            case NOT_NULL:
                result = cb.isNotNull(resolveFieldPath(root, filterField.getField()));
                break;
            case GREATER_THAN:
                result = gt(cb, resolveFieldPath(root, filterField.getField()), filterField.getValue());
                break;

            case GREATER_THAN_EQUAL:
                if (filterField.getEnumName() != null && filterField.getEnumName().equals(EnumName.JS_DATE)) {
                    result = ge(cb, resolveFieldPath(root, filterField.getField()),
                            Helper.getDateFromJsString(filterField.getValue().toString()));
                } else {
                    result = ge(cb, resolveFieldPath(root, filterField.getField()), filterField.getValue());
                }
                break;

            case LESS_THAN:
                result = lt(cb, resolveFieldPath(root, filterField.getField()), filterField.getValue());
                break;

            case LESS_THAN_EQUAL:
                if (filterField.getEnumName() != null && filterField.getEnumName().equals(EnumName.JS_DATE)) {
                    result = le(cb, resolveFieldPath(root, filterField.getField()),
                            Helper.getDateFromJsString(filterField.getValue().toString()));
                } else {
                    result = le(cb, resolveFieldPath(root, filterField.getField()), filterField.getValue());
                }
                break;
            case IN: {
                Path<?> path = resolveFieldPath(root, filterField.getField());
                CriteriaBuilder.In<Object> inClause;

                if (path.getJavaType().isAnnotationPresent(jakarta.persistence.Entity.class)) {
                    inClause = cb.in(path.get("id"));
                } else {
                    inClause = cb.in(path);
                }
                filterField.getValues().forEach(value -> inClause.value(getRealValue(filterField.getEnumName(),
                        value)));
                result = inClause;
                break;
            }
            case IN_MANY_TO_MANY:
                Join<Object, Object> join = root.join(filterField.getField(), filterField.getJoinType());
                CriteriaBuilder.In<Object> inClause1 = cb.in(join.get("id"));
                filterField.getValues().forEach(value -> inClause1.value(getRealValue(filterField.getEnumName(),
                        value)));
                result = inClause1;
                break;
        }
        return wrapAlternatives(result, root, query, cb);
    }

    private Predicate wrapAlternatives(Predicate result, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        if (filterField.getAlternatives() == null || filterField.getAlternatives().size() == 0) {
            return result;
        } else {
            List<SpecificationBuilder<T>> specificationBuilders =
                    filterField.getAlternatives().stream().map(alternative -> {
                        SpecificationBuilder<T> builder = new SpecificationBuilder<>();
                        builder.with(alternative);
                        return builder;
                    }).collect(Collectors.toList());
            List<Predicate> predicates =
                    specificationBuilders.stream().map(specificationBuilder -> specificationBuilder.build().toPredicate(root, query, cb)).collect(Collectors.toList());
            predicates.add(result);
            Predicate[] predicatesArray = predicates.toArray(new Predicate[0]);
            return cb.or(predicatesArray);
        }
    }

    private Object getRealValue(EnumName enumName, Object value) {
        if (enumName == null) {
            return value;
        }
        if (value instanceof String) {
            switch (enumName) {
                case PRIORITY:
                    return Priority.getPriorityFromString(value.toString());
                case STATUS:
                    return Status.getStatusFromString(value.toString());
                case JS_DATE:
                    return Helper.getDateFromJsString(value.toString());
                default:
                    return value;
            }
        }
        return value;
    }

    private Predicate buildLikePredicate(Root<T> root, CriteriaBuilder cb, String patternWithWildcards) {
        Path<?> fieldPath = resolveFieldPath(root, filterField.getField());
        Expression<String> field = (Expression<String>) fieldPath;
        Expression<String> unaccentedField = cb.function("unaccent", String.class,
                cb.lower(field));
        Expression<String> unaccentedPattern = cb.function("unaccent", String.class,
                cb.literal(patternWithWildcards));
        return cb.like(unaccentedField, unaccentedPattern);
    }

    private Path<?> resolveFieldPath(Root<T> root, String field) {
        String[] fieldNames = field.split("\\.");

        From<?, ?> currentFrom = root;
        Path<?> currentPath = root;

        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];

            if (isCollectionAttribute(currentFrom, fieldName)) {
                String joinKey = buildJoinKey(fieldNames, i);
                Join<?, ?> join = joinCache.get(joinKey);
                if (join == null) {
                    join = currentFrom.join(fieldName, JoinType.LEFT);
                    joinCache.put(joinKey, join);
                }
                currentFrom = join;
                currentPath = join;
            } else {
                currentPath = currentPath.get(fieldName);
                if (currentPath instanceof From) {
                    currentFrom = (From<?, ?>) currentPath;
                }
            }
        }
        return currentPath;
    }

    private boolean isCollectionAttribute(From<?, ?> from, String attributeName) {
        try {
            ManagedType<?> managedType = null;
            if (from instanceof Root) {
                managedType = ((Root<?>) from).getModel();
            } else if (from.getModel() instanceof ManagedType) {
                managedType = (ManagedType<?>) from.getModel();
            }
            if (managedType != null) {
                Attribute<?, ?> attr = managedType.getAttribute(attributeName);
                return attr.isCollection();
            }
        } catch (IllegalArgumentException e) {
            // Attribute not found, fall through
        }
        return false;
    }

    private String buildJoinKey(String[] fieldNames, int upToIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= upToIndex; i++) {
            if (i > 0) sb.append(".");
            sb.append(fieldNames[i]);
        }
        return sb.toString();
    }

    @SuppressWarnings({"unchecked"})
    private <Y extends Comparable<? super Y>> Predicate le(CriteriaBuilder cb, Path<?> path, Object value) {
        return cb.lessThanOrEqualTo((Expression<Y>) path, (Y) value);
    }

    @SuppressWarnings({"unchecked"})
    private <Y extends Comparable<? super Y>> Predicate ge(CriteriaBuilder cb, Path<?> path, Object value) {
        return cb.greaterThanOrEqualTo((Expression<Y>) path, (Y) value);
    }

    @SuppressWarnings({"unchecked"})
    private <Y extends Comparable<? super Y>> Predicate lt(CriteriaBuilder cb, Path<?> path, Object value) {
        return cb.lessThan((Expression<Y>) path, (Y) value);
    }

    @SuppressWarnings({"unchecked"})
    private <Y extends Comparable<? super Y>> Predicate gt(CriteriaBuilder cb, Path<?> path, Object value) {
        return cb.greaterThan((Expression<Y>) path, (Y) value);
    }
}

