package com.grash.automation;

import com.grash.automation.action.ActionHandler;
import com.grash.automation.dto.AutomationActionPostDTO;
import com.grash.automation.dto.AutomationConditionPostDTO;
import com.grash.automation.dto.AutomationRulePostDTO;
import com.grash.automation.event.ChangeType;
import com.grash.automation.event.EntityType;
import com.grash.automation.eval.CustomFieldResolver;
import com.grash.automation.eval.ExecutionContext;
import com.grash.automation.eval.OperandResolver;
import com.grash.automation.model.ActionType;
import com.grash.automation.model.AutomationActionStep;
import com.grash.automation.model.AutomationCondition;
import com.grash.automation.model.AutomationRule;
import com.grash.automation.model.ConditionOperator;
import com.grash.automation.repository.AutomationRuleRepository;
import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.CompanySettings;
import com.grash.model.CustomField;
import com.grash.repository.CustomFieldRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers what a rule is allowed to be. Everything refused here is something the old engine would
 * have stored and then ignored — a condition nothing can read, an action nothing carries out, or a
 * comparison that can never come out true.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutomationRuleServiceTest {

    @Mock
    private AutomationRuleRepository ruleRepository;
    @Mock
    private CustomFieldRepository customFieldRepository;

    private AutomationRuleService service;
    private Company company;

    /** Reads whatever subject the test names, so the service's resolvability check passes. */
    private static class AnyAssetResolver implements OperandResolver {
        @Override
        public boolean supports(String subject) {
            return subject.startsWith("asset.");
        }

        @Override
        public Object resolve(AutomationCondition condition, ExecutionContext context) {
            return null;
        }

        @Override
        public List<com.grash.automation.eval.OperandDescriptor> describe(Company company) {
            return List.of();
        }
    }

    private static class CreateWorkOrderStub implements ActionHandler {
        @Override
        public ActionType getType() {
            return ActionType.CREATE_WORK_ORDER;
        }

        @Override
        public com.grash.automation.action.ActionDescriptor descriptor() {
            // The real handler's parameter list, because the service now validates against it:
            // a stub declaring no parameters would make every rule below fail on "title".
            return new com.grash.automation.action.ActionDescriptor(ActionType.CREATE_WORK_ORDER,
                    "automation_action_create_work_order", List.of(
                    com.grash.automation.action.ActionDescriptor.Parameter.text("title", true),
                    com.grash.automation.action.ActionDescriptor.Parameter.enumOf("priority", false,
                            List.of("NONE", "LOW", "MEDIUM", "HIGH")),
                    com.grash.automation.action.ActionDescriptor.Parameter.entity("category",
                            "WORK_ORDER_CATEGORY", false),
                    com.grash.automation.action.ActionDescriptor.Parameter.triggerReference("asset",
                            false)));
        }

        @Override
        public void execute(AutomationActionStep step, ExecutionContext context) {
        }
    }

    @BeforeEach
    void setUp() {
        service = new AutomationRuleService(ruleRepository, customFieldRepository,
                List.of(new AnyAssetResolver()), List.of(new CreateWorkOrderStub()));

        company = new Company();
        company.setId(9L);
        CompanySettings settings = new CompanySettings();
        settings.setId(9L);
        settings.setCompany(company);
        company.setCompanySettings(settings);

        when(ruleRepository.save(any(AutomationRule.class))).thenAnswer(call -> call.getArgument(0));
    }

    private CustomField choiceField(Long id, String label, List<String> options) {
        CustomField field = new CustomField();
        field.setId(id);
        field.setLabel(label);
        field.setFieldType(com.grash.model.enums.CustomFieldType.SINGLE_CHOICE);
        field.setOptions(options);
        field.setCompanySettings(company.getCompanySettings());
        when(customFieldRepository.findById(id)).thenReturn(Optional.of(field));
        return field;
    }

    private CustomField textField(Long id) {
        CustomField field = new CustomField();
        field.setId(id);
        field.setLabel("Notiz");
        field.setFieldType(com.grash.model.enums.CustomFieldType.SHORT_TEXT);
        field.setCompanySettings(company.getCompanySettings());
        when(customFieldRepository.findById(id)).thenReturn(Optional.of(field));
        return field;
    }

    private AutomationRulePostDTO ruleWith(AutomationConditionPostDTO condition) {
        return new AutomationRulePostDTO(
                "Kritische Anlage fällt aus",
                ChangeType.UPDATED,
                EntityType.ASSET,
                java.util.Set.of("status"),
                null,
                null,
                List.of(condition),
                List.of(new AutomationActionPostDTO(ActionType.CREATE_WORK_ORDER,
                        "{\"title\":\"Störung\"}", null, null)));
    }

    @Nested
    @DisplayName("a condition on a single-choice custom field")
    class ChoiceFieldValues {

        @Test
        @DisplayName("is refused when the value is not one of the options")
        void refusesAnImpossibleValue() {
            // The mistake that produced this test: a rule compared an "Assetclass" field to "A"
            // while its options were 1-Critical / 2-Operational Critical / 3-Support. It saved,
            // it ran, and it skipped every single time with a perfectly accurate explanation of
            // a comparison that was never winnable.
            choiceField(202L, "Assetclass",
                    List.of("1-Critical", "2-Operational Critical", "3-Support"));

            CustomException exception = assertThrows(CustomException.class, () ->
                    service.create(ruleWith(new AutomationConditionPostDTO(
                            CustomFieldResolver.SUBJECT, 202L, ConditionOperator.IS, "A")), company));

            assertEquals(422, exception.getHttpStatus().value());
            assertTrue(exception.getMessage().contains("1-Critical"),
                    "the message has to list what would work: " + exception.getMessage());
            verify(ruleRepository, never()).save(any());
        }

        @Test
        void acceptsAnActualOption() {
            choiceField(202L, "Assetclass",
                    List.of("1-Critical", "2-Operational Critical", "3-Support"));

            AutomationRule rule = service.create(ruleWith(new AutomationConditionPostDTO(
                    CustomFieldResolver.SUBJECT, 202L, ConditionOperator.IS, "1-Critical")), company);

            assertEquals(1, rule.getConditions().size());
            assertEquals("1-Critical", rule.getConditions().get(0).getExpectedValue());
        }

        @Test
        @DisplayName("CONTAINS may match across options, so it is not checked")
        void allowsASubstringWithContains() {
            choiceField(202L, "Assetclass",
                    List.of("1-Critical", "2-Operational Critical", "3-Support"));

            AutomationRule rule = service.create(ruleWith(new AutomationConditionPostDTO(
                    CustomFieldResolver.SUBJECT, 202L, ConditionOperator.CONTAINS, "Critical")), company);

            assertEquals("Critical", rule.getConditions().get(0).getExpectedValue());
        }

        @Test
        @DisplayName("a free-text field takes any value")
        void doesNotCheckTextFields() {
            textField(203L);

            AutomationRule rule = service.create(ruleWith(new AutomationConditionPostDTO(
                    CustomFieldResolver.SUBJECT, 203L, ConditionOperator.IS, "irgendwas")), company);

            assertEquals("irgendwas", rule.getConditions().get(0).getExpectedValue());
        }
    }

    @Nested
    @DisplayName("a rule the engine could not carry out")
    class Unrunnable {

        @Test
        @DisplayName("is refused when no resolver can read the subject")
        void refusesAnUnreadableSubject() {
            CustomException exception = assertThrows(CustomException.class, () ->
                    service.create(ruleWith(new AutomationConditionPostDTO(
                            "workOrder.nonsense", null, ConditionOperator.IS, "x")), company));

            assertEquals(422, exception.getHttpStatus().value());
            assertTrue(exception.getMessage().contains("workOrder.nonsense"), exception.getMessage());
        }

        @Test
        @DisplayName("is refused when no handler implements the action")
        void refusesAnUnhandledAction() {
            AutomationRulePostDTO dto = new AutomationRulePostDTO(
                    "Benachrichtigen", ChangeType.UPDATED, EntityType.ASSET, null, null, null,
                    List.of(),
                    // NOTIFY is a real action type, but this service was built with only the
                    // work-order handler, which is what a half-deployed engine looks like.
                    List.of(new AutomationActionPostDTO(ActionType.NOTIFY, "{}", null, null)));

            CustomException exception = assertThrows(CustomException.class,
                    () -> service.create(dto, company));

            assertEquals(422, exception.getHttpStatus().value());
            assertTrue(exception.getMessage().contains("NOTIFY"), exception.getMessage());
        }

        @Test
        void refusesParametersThatAreNotAJsonObject() {
            AutomationRulePostDTO dto = new AutomationRulePostDTO(
                    "Kaputt", ChangeType.UPDATED, EntityType.ASSET, null, null, null,
                    List.of(),
                    List.of(new AutomationActionPostDTO(ActionType.CREATE_WORK_ORDER,
                            "[\"nicht\",\"ein\",\"objekt\"]", null, null)));

            assertThrows(CustomException.class, () -> service.create(dto, company));
        }
    }

    @Nested
    @DisplayName("action parameters")
    class Parameters {

        private AutomationRulePostDTO withParameters(String json) {
            return new AutomationRulePostDTO(
                    "Auftrag anlegen", ChangeType.UPDATED, EntityType.ASSET, null, null, null,
                    List.of(),
                    List.of(new AutomationActionPostDTO(ActionType.CREATE_WORK_ORDER, json, null,
                            null)));
        }

        @Test
        @DisplayName("a required one that is missing is refused")
        void refusesAMissingRequiredParameter() {
            CustomException exception = assertThrows(CustomException.class,
                    () -> service.create(withParameters("{\"priority\":\"HIGH\"}"), company));

            assertEquals(422, exception.getHttpStatus().value());
            assertTrue(exception.getMessage().contains("title"), exception.getMessage());
        }

        @Test
        @DisplayName("a misspelled key is refused rather than ignored")
        void refusesAnUnknownParameter() {
            // Without this the handler reads its own key, finds nothing, and reports the required
            // parameter missing — from a background thread, minutes later, in the run log. The
            // typo itself is never mentioned.
            CustomException exception = assertThrows(CustomException.class, () -> service.create(
                    withParameters("{\"title\":\"St\u00f6rung\",\"titel\":\"x\"}"), company));

            assertEquals(422, exception.getHttpStatus().value());
            assertTrue(exception.getMessage().contains("titel"), exception.getMessage());
        }

        @Test
        @DisplayName("a value outside an enum parameter's options is refused")
        void refusesAnImpossibleEnumValue() {
            CustomException exception = assertThrows(CustomException.class, () -> service.create(
                    withParameters("{\"title\":\"x\",\"priority\":\"URGENT\"}"), company));

            assertEquals(422, exception.getHttpStatus().value());
            assertTrue(exception.getMessage().contains("HIGH"),
                    "the message has to list what would work: " + exception.getMessage());
        }

        @Test
        void refusesAnUnknownPlaceholder() {
            CustomException exception = assertThrows(CustomException.class, () -> service.create(
                    withParameters("{\"title\":\"${trigger.asset.serial}\"}"), company));

            assertEquals(422, exception.getHttpStatus().value());
            assertTrue(exception.getMessage().contains("trigger.asset.serial"),
                    exception.getMessage());
        }

        @Test
        @DisplayName("a placeholder in a parameter that cannot carry one is refused")
        void refusesAPlaceholderWhereItCannotWork() {
            // Interpolating an asset id into a title is the point of the mechanism. Doing it in a
            // category reference produces a lookup for a category id that does not exist.
            CustomException exception = assertThrows(CustomException.class, () -> service.create(
                    withParameters("{\"title\":\"x\",\"category\":\"${trigger.asset.id}\"}"),
                    company));

            assertEquals(422, exception.getHttpStatus().value());
            assertTrue(exception.getMessage().contains("category"), exception.getMessage());
        }

        @Test
        @DisplayName("a valid set, placeholders included, is accepted")
        void acceptsWhatTheHandlerAsksFor() {
            AutomationRule rule = service.create(withParameters(
                    "{\"title\":\"St\u00f6rung ${trigger.asset.name}\",\"priority\":\"HIGH\","
                            + "\"asset\":\"${trigger.asset.id}\"}"), company);

            assertEquals(1, rule.getActions().size());
            assertTrue(rule.getActions().get(0).getParameters().contains("trigger.asset.name"));
        }
    }

    @Nested
    @DisplayName("customFieldId")
    class CustomFieldReference {

        @Test
        void isRequiredForACustomFieldSubject() {
            assertThrows(CustomException.class, () ->
                    service.create(ruleWith(new AutomationConditionPostDTO(
                            CustomFieldResolver.SUBJECT, null, ConditionOperator.IS, "x")), company));
        }

        @Test
        void isRejectedOnANativeSubject() {
            assertThrows(CustomException.class, () ->
                    service.create(ruleWith(new AutomationConditionPostDTO(
                            "asset.status", 202L, ConditionOperator.IS, "DOWN")), company));
        }

        @Test
        void mustBelongToTheSameCompany() {
            Company other = new Company();
            other.setId(77L);
            CompanySettings otherSettings = new CompanySettings();
            otherSettings.setCompany(other);
            CustomField foreign = new CustomField();
            foreign.setId(999L);
            foreign.setLabel("Fremd");
            foreign.setFieldType(com.grash.model.enums.CustomFieldType.SHORT_TEXT);
            foreign.setCompanySettings(otherSettings);
            when(customFieldRepository.findById(999L)).thenReturn(Optional.of(foreign));

            CustomException exception = assertThrows(CustomException.class, () ->
                    service.create(ruleWith(new AutomationConditionPostDTO(
                            CustomFieldResolver.SUBJECT, 999L, ConditionOperator.IS, "x")), company));

            assertEquals(403, exception.getHttpStatus().value());
        }
    }
}
