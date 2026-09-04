package com.grash.automation.eval;

import com.grash.automation.event.ChangeType;
import com.grash.automation.event.EntityChangedEvent;
import com.grash.automation.event.EntityType;
import com.grash.automation.model.AutomationCondition;
import com.grash.automation.model.AutomationRule;
import com.grash.automation.model.ConditionOperator;
import com.grash.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The evaluator has no Spring and no database in it, which is the point of pushing all reading
 * into resolvers: the decision logic can be tested as a function over a map of operand values.
 */
class RuleEvaluatorTest {

    /** Stands in for the resolvers; returns whatever the test put in the map. */
    private static class StubResolver implements OperandResolver {
        private final Map<String, Object> values;

        StubResolver(Map<String, Object> values) {
            this.values = values;
        }

        @Override
        public boolean supports(String subject) {
            return values.containsKey(subject);
        }

        @Override
        public List<OperandDescriptor> describe(com.grash.model.Company company) {
            return List.of();
        }

        @Override
        public Object resolve(AutomationCondition condition, ExecutionContext context) {
            return values.get(condition.getSubject());
        }
    }

    private RuleEvaluator evaluatorOver(Map<String, Object> operands) {
        return new RuleEvaluator(List.of(new StubResolver(operands)));
    }

    private ExecutionContext contextWithChangedFields(String... changedFields) {
        return new ExecutionContext(
                EntityChangedEvent.root(ChangeType.UPDATED, EntityType.ASSET, 1L, 9L,
                        Set.of(changedFields), null),
                null,
                null);
    }

    private AutomationRule ruleWith(AutomationCondition... conditions) {
        AutomationRule rule = new AutomationRule();
        for (AutomationCondition condition : conditions) {
            rule.addCondition(condition);
        }
        return rule;
    }

    private AutomationCondition condition(String subject, ConditionOperator operator, String expected) {
        AutomationCondition condition = new AutomationCondition();
        condition.setSubject(subject);
        condition.setOperator(operator);
        condition.setExpectedValue(expected);
        return condition;
    }

    @Nested
    @DisplayName("comparison operators")
    class Operators {

        @Test
        void isMatchesEqualValues() {
            RuleEvaluator evaluator = evaluatorOver(Map.of("asset.status", "DOWN"));

            assertNull(evaluator.firstUnmetCondition(
                    ruleWith(condition("asset.status", ConditionOperator.IS, "DOWN")),
                    contextWithChangedFields("status")));
        }

        @Test
        void isDoesNotMatchDifferentValues() {
            RuleEvaluator evaluator = evaluatorOver(Map.of("asset.status", "OPERATIONAL"));

            assertNotNull(evaluator.firstUnmetCondition(
                    ruleWith(condition("asset.status", ConditionOperator.IS, "DOWN")),
                    contextWithChangedFields("status")));
        }

        @Test
        void isNotIsTheInverse() {
            RuleEvaluator evaluator = evaluatorOver(Map.of("asset.status", "OPERATIONAL"));

            assertNull(evaluator.firstUnmetCondition(
                    ruleWith(condition("asset.status", ConditionOperator.IS_NOT, "DOWN")),
                    contextWithChangedFields("status")));
        }

        @Test
        @DisplayName("a numeric operand compares as text, so an id can be matched without casting")
        void numbersCompareAsText() {
            RuleEvaluator evaluator = evaluatorOver(Map.of("asset.category", 7L));

            assertNull(evaluator.firstUnmetCondition(
                    ruleWith(condition("asset.category", ConditionOperator.IS, "7")),
                    contextWithChangedFields("category")));
        }

        @Test
        void containsIsASubstring() {
            RuleEvaluator evaluator = evaluatorOver(Map.of("asset.name", "Pumpe P-12"));

            assertNull(evaluator.firstUnmetCondition(
                    ruleWith(condition("asset.name", ConditionOperator.CONTAINS, "Pumpe")),
                    contextWithChangedFields("name")));
        }

        @Test
        @DisplayName("an operand the entity has no value for does not match, and does not throw")
        void containsOnAMissingValueDoesNotMatch() {
            // Null is an ordinary answer from a resolver — an asset simply may have no value for
            // a custom field — so it has to read as "condition not met" and not as a failure.
            Map<String, Object> operands = new java.util.HashMap<>();
            operands.put("asset.cf", null);
            RuleEvaluator evaluator = new RuleEvaluator(List.of(new StubResolver(operands)));

            assertNotNull(evaluator.firstUnmetCondition(
                    ruleWith(condition("asset.cf", ConditionOperator.CONTAINS, "A")),
                    contextWithChangedFields("name")));
        }
    }

    @Nested
    @DisplayName("CHANGED_TO")
    class ChangedTo {

        @Test
        @DisplayName("holds when the field is in the diff and now has the expected value")
        void holdsWhenTheFieldChanged() {
            RuleEvaluator evaluator = evaluatorOver(Map.of("asset.status", "DOWN"));

            assertNull(evaluator.firstUnmetCondition(
                    ruleWith(condition("asset.status", ConditionOperator.CHANGED_TO, "DOWN")),
                    contextWithChangedFields("status")));
        }

        @Test
        @DisplayName("does not hold when the value matches but the field did not change")
        void doesNotHoldWithoutAChange() {
            // The case that separates CHANGED_TO from IS, and the reason the event carries a
            // diff at all: an asset that was already DOWN must not re-trigger a rule on every
            // unrelated edit.
            RuleEvaluator evaluator = evaluatorOver(Map.of("asset.status", "DOWN"));

            assertNotNull(evaluator.firstUnmetCondition(
                    ruleWith(condition("asset.status", ConditionOperator.CHANGED_TO, "DOWN")),
                    contextWithChangedFields("name")));
        }
    }

    @Nested
    @DisplayName("a rule's conditions")
    class Conjunction {

        @Test
        void allHaveToHold() {
            RuleEvaluator evaluator = evaluatorOver(Map.of(
                    "asset.status", "DOWN",
                    "asset.name", "Lüftung L-3"));

            String unmet = evaluator.firstUnmetCondition(
                    ruleWith(condition("asset.status", ConditionOperator.IS, "DOWN"),
                            condition("asset.name", ConditionOperator.CONTAINS, "Pumpe")),
                    contextWithChangedFields("status"));

            assertNotNull(unmet);
            assertTrue(unmet.contains("asset.name"), "the reason names the condition that failed: " + unmet);
        }

        @Test
        @DisplayName("a rule without conditions always fires")
        void noConditionsMeansAlways() {
            assertNull(evaluatorOver(Map.of()).firstUnmetCondition(ruleWith(),
                    contextWithChangedFields("status")));
        }
    }

    @Nested
    @DisplayName("an operand nothing can read")
    class UnreadableSubject {

        @Test
        @DisplayName("throws instead of quietly reporting 'not met'")
        void failsLoudly() {
            // This is the whole lesson of TITLE_CONTAINS in the old engine: an unhandled subject
            // fell through to `default: return false`, which is indistinguishable from a
            // condition that legitimately did not hold — and because every condition has to
            // hold, one of them silently disabled the entire rule. Here it is an error, the run
            // is recorded as FAILED, and the message says which subject.
            RuleEvaluator evaluator = evaluatorOver(Map.of("asset.status", "DOWN"));

            CustomException exception = assertThrows(CustomException.class, () ->
                    evaluator.firstUnmetCondition(
                            ruleWith(condition("asset.nonsense", ConditionOperator.IS, "x")),
                            contextWithChangedFields("status")));

            assertTrue(exception.getMessage().contains("asset.nonsense"), exception.getMessage());
            assertEquals(422, exception.getHttpStatus().value());
        }
    }
}
