package com.grash.automation;

import com.grash.automation.action.ActionDescriptor;
import com.grash.automation.action.ActionHandler;
import com.grash.automation.dto.AutomationMetaDTO;
import com.grash.automation.event.ChangeType;
import com.grash.automation.event.EntityType;
import com.grash.automation.eval.ExecutionContext;
import com.grash.automation.eval.OperandDescriptor;
import com.grash.automation.eval.OperandResolver;
import com.grash.automation.model.ActionType;
import com.grash.automation.model.AutomationActionStep;
import com.grash.automation.model.AutomationCondition;
import com.grash.automation.model.ConditionOperator;
import com.grash.model.Company;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The editor's metadata is not a document someone maintains — it is the registered resolvers and
 * handlers, read back. These tests pin that property, because the moment the endpoint starts
 * carrying a list of its own it has reintroduced the defect it was built to remove.
 */
class AutomationMetaServiceTest {

    private static class OneSubjectResolver implements OperandResolver {
        @Override
        public boolean supports(String subject) {
            return "asset.status".equals(subject);
        }

        @Override
        public List<OperandDescriptor> describe(Company company) {
            return List.of(OperandDescriptor.native_("asset.status", "ENUM",
                    List.of(ConditionOperator.IS), List.of("DOWN")));
        }

        @Override
        public Object resolve(AutomationCondition condition, ExecutionContext context) {
            return null;
        }
    }

    private static class OneActionHandler implements ActionHandler {
        @Override
        public ActionType getType() {
            return ActionType.NOTIFY;
        }

        @Override
        public ActionDescriptor descriptor() {
            return new ActionDescriptor(ActionType.NOTIFY, "automation_action_notify",
                    List.of(ActionDescriptor.Parameter.text("message", true)));
        }

        @Override
        public void execute(AutomationActionStep step, ExecutionContext context) {
        }
    }

    private AutomationMetaDTO describe(boolean engineEnabled) {
        AutomationMetaService service = new AutomationMetaService(
                List.of(new OneSubjectResolver()), List.of(new OneActionHandler()));
        ReflectionTestUtils.setField(service, "engineEnabled", engineEnabled);
        return service.describe(new Company());
    }

    @Nested
    @DisplayName("the vocabulary")
    class Vocabulary {

        @Test
        @DisplayName("is exactly what the registered components report, and nothing besides")
        void comesFromTheComponents() {
            AutomationMetaDTO meta = describe(true);

            assertEquals(List.of("asset.status"), meta.subjects().stream()
                    .map(OperandDescriptor::subject).toList());
            assertEquals(List.of(ActionType.NOTIFY), meta.actions().stream()
                    .map(ActionDescriptor::type).toList());
            // CREATE_WORK_ORDER exists as an enum value but has no handler in this setup, which
            // is what a half-deployed engine looks like. It must not be offered.
            assertFalse(meta.actions().stream().anyMatch(action -> action.type() == ActionType.CREATE_WORK_ORDER));
        }

        @Test
        @DisplayName("names every placeholder a text parameter may use")
        void listsPlaceholders() {
            assertTrue(describe(true).placeholders().contains("${trigger.asset.id}"),
                    "the one placeholder every asset rule needs");
        }
    }

    @Nested
    @DisplayName("triggers")
    class Triggers {

        @Test
        @DisplayName("cover every combination, so the editor never silently omits one")
        void areComplete() {
            assertEquals(EntityType.values().length * ChangeType.values().length,
                    describe(true).triggers().size());
        }

        @Test
        @DisplayName("are marked live only where a service actually publishes the event")
        void distinguishWiredFromUnwired() {
            List<AutomationMetaDTO.Trigger> live = describe(true).triggers().stream()
                    .filter(AutomationMetaDTO.Trigger::live).toList();

            // Only the asset status change is published so far. If this assertion fails after a
            // new publish point was added, the fix is to extend LIVE_TRIGGERS and this number —
            // that is the intended coupling, not an inconvenience.
            assertEquals(1, live.size());
            assertEquals(EntityType.ASSET, live.get(0).entityType());
            assertEquals(ChangeType.UPDATED, live.get(0).changeType());
            assertEquals(List.of("status"), live.get(0).changedFields(),
                    "the editor may only offer field filters the diff can actually report");
        }
    }

    @Test
    @DisplayName("the engine's on/off state travels with the metadata")
    void reportsWhetherTheEngineIsOn() {
        // Without this the editor cannot tell a rule that never matched from a rule that was
        // never reached, and AUTOMATION_ENABLED defaults to false.
        assertFalse(describe(false).engineEnabled());
        assertTrue(describe(true).engineEnabled());
    }
}
