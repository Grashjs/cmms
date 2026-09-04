package com.grash.automation.action;

import com.grash.automation.event.ChangeType;
import com.grash.automation.event.EntityChangedEvent;
import com.grash.automation.event.EntityType;
import com.grash.automation.eval.ExecutionContext;
import com.grash.exception.CustomException;
import com.grash.model.Asset;
import com.grash.model.enums.AssetStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the runtime-value substitution, which is the capability the old engine is missing: its
 * actions can only carry references fixed when the rule was configured, so "a work order for
 * <em>this</em> asset" is inexpressible there.
 */
class ActionParametersTest {

    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        Asset asset = new Asset();
        asset.setId(42L);
        asset.setName("Pumpe P-12");
        asset.setStatus(AssetStatus.DOWN);
        context = new ExecutionContext(
                EntityChangedEvent.root(ChangeType.UPDATED, EntityType.ASSET, 42L, 9L,
                        Set.of("status"), null),
                null,
                asset);
    }

    @Nested
    @DisplayName("placeholders")
    class Placeholders {

        @Test
        @DisplayName("a placeholder that is the whole value keeps its type")
        void wholeValueKeepsItsType() {
            // So a reference does not have to be parsed back out of a string. This is what lets
            // "asset": "${trigger.asset.id}" be handed to a setter that wants a number.
            ActionParameters parameters = ActionParameters.of(
                    "{\"asset\":\"${trigger.asset.id}\"}", context);

            assertEquals(42L, parameters.requireLong("asset"));
        }

        @Test
        @DisplayName("inside a longer string it is interpolated as text")
        void interpolatesIntoText() {
            ActionParameters parameters = ActionParameters.of(
                    "{\"title\":\"Störung ${trigger.asset.name}\"}", context);

            assertEquals("Störung Pumpe P-12", parameters.requireString("title"));
        }

        @Test
        void resolvesSeveralInOneString() {
            ActionParameters parameters = ActionParameters.of(
                    "{\"message\":\"${trigger.asset.name} ist ${trigger.asset.status}\"}", context);

            assertEquals("Pumpe P-12 ist DOWN", parameters.requireString("message"));
        }

        @Test
        @DisplayName("an unknown placeholder is an error, not an empty string")
        void unknownPlaceholderThrows() {
            // Substituting nothing would produce a work order with no asset and no complaint,
            // which is the failure mode this engine exists to avoid.
            CustomException exception = assertThrows(CustomException.class, () ->
                    ActionParameters.of("{\"asset\":\"${trigger.asset.serial}\"}", context));

            assertTrue(exception.getMessage().contains("trigger.asset.serial"), exception.getMessage());
        }

        @Test
        void nonStringValuesArePassedThrough() {
            ActionParameters parameters = ActionParameters.of(
                    "{\"category\":7,\"abortOnFailure\":true}", context);

            assertEquals(7L, parameters.requireLong("category"));
        }
    }

    @Nested
    @DisplayName("reading values")
    class Reading {

        @Test
        void missingOptionalValueIsNull() {
            ActionParameters parameters = ActionParameters.of("{\"title\":\"x\"}", context);

            assertNull(parameters.getString("priority"));
            assertNull(parameters.getLong("category"));
        }

        @Test
        void missingRequiredValueThrows() {
            ActionParameters parameters = ActionParameters.of("{}", context);

            assertThrows(CustomException.class, () -> parameters.requireString("title"));
            assertThrows(CustomException.class, () -> parameters.requireLong("customField"));
        }

        @Test
        void aNumberGivenAsTextStillReadsAsANumber() {
            ActionParameters parameters = ActionParameters.of("{\"team\":\"12\"}", context);

            assertEquals(12L, parameters.requireLong("team"));
        }

        @Test
        void somethingThatIsNotANumberThrows() {
            ActionParameters parameters = ActionParameters.of("{\"team\":\"Schichtleitung\"}", context);

            CustomException exception = assertThrows(CustomException.class,
                    () -> parameters.requireLong("team"));
            assertTrue(exception.getMessage().contains("team"), exception.getMessage());
        }
    }

    @Nested
    @DisplayName("malformed input")
    class Malformed {

        @Test
        void emptyParametersAreAllowed() {
            assertNull(ActionParameters.of(null, context).getString("title"));
            assertNull(ActionParameters.of("   ", context).getString("title"));
        }

        @Test
        void brokenJsonThrows() {
            assertThrows(CustomException.class, () -> ActionParameters.of("{not json", context));
        }
    }

    @Nested
    @DisplayName("a rule not triggered by an asset")
    class WrongTriggerType {

        @Test
        @DisplayName("says so instead of substituting nothing")
        void assetPlaceholderOnANonAssetThrows() {
            ExecutionContext workOrderContext = new ExecutionContext(
                    EntityChangedEvent.root(ChangeType.CREATED, EntityType.WORK_ORDER, 1L, 9L,
                            Set.of(), null),
                    null,
                    "not an asset");

            assertThrows(CustomException.class, () ->
                    ActionParameters.of("{\"asset\":\"${trigger.asset.id}\"}", workOrderContext));
        }
    }
}
