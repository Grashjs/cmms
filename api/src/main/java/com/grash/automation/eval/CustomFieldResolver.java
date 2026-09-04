package com.grash.automation.eval;

import com.grash.automation.model.AutomationCondition;
import com.grash.automation.model.ConditionOperator;
import com.grash.exception.CustomException;
import com.grash.model.Asset;
import com.grash.model.AssetCategory;
import com.grash.model.Company;
import com.grash.model.CustomField;
import com.grash.model.CustomFieldValue;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.CustomFieldType;
import com.grash.repository.CustomFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reads a custom field value off the triggering asset — the resolver the leading use case needs,
 * because "asset class" is a custom field and nothing native.
 *
 * <p>Two things are worth knowing here. Custom fields carry <b>no company column</b>:
 * {@code CustomField} and {@code CustomFieldValue} extend {@code Audit}, not
 * {@code CompanyAudit}, so tenancy has to be checked through the owning company settings rather
 * than read off the row. And a field can be <b>bound to asset categories</b>: for an asset of
 * another category there simply is no value, so the condition does not hold. That is correct but
 * easy to misread as a broken rule, which is why the editor has to show the binding.
 */
@Component
@RequiredArgsConstructor
public class CustomFieldResolver implements OperandResolver {

    public static final String SUBJECT = "asset.cf";

    private final CustomFieldRepository customFieldRepository;

    @Override
    public boolean supports(String subject) {
        return SUBJECT.equals(subject);
    }

    /**
     * Every asset custom field this company defined, each as its own operand.
     *
     * <p>A choice field reports its options, which is what lets the editor offer a dropdown
     * instead of a text box — and a text box is how a rule ends up comparing "Assetclass" to a
     * value that field can never hold.
     */
    @Override
    public List<OperandDescriptor> describe(Company company) {
        return customFieldRepository
                .findByCompanySettingsAndEntityTypeFetchAssetCategories(
                        company.getCompanySettings(), CustomFieldEntityType.ASSET)
                .stream()
                .map(field -> new OperandDescriptor(
                        SUBJECT,
                        field.getId(),
                        null,
                        // The field's own name, not a translation key: it is data the user typed.
                        field.getLabel(),
                        field.getFieldType() == CustomFieldType.SINGLE_CHOICE ? "CHOICE" : "TEXT",
                        OPERATORS,
                        field.getFieldType() == CustomFieldType.SINGLE_CHOICE
                                ? field.getOptions() : List.of(),
                        field.getAssetCategories().stream().map(AssetCategory::getName).toList()))
                .toList();
    }

    /**
     * The same three for every field type, and that is worth stating rather than looking like an
     * oversight. CONTAINS earns its place even on a choice field, because matching "Critical"
     * across "1-Critical" and "2-Operational Critical" is a real thing to ask for. CHANGED_TO is
     * absent for all of them: the event diff reports native fields only, so a rule using it on a
     * custom field could never hold, and an operator that cannot work is not offered.
     */
    private static final List<ConditionOperator> OPERATORS =
            List.of(ConditionOperator.IS, ConditionOperator.IS_NOT, ConditionOperator.CONTAINS);

    @Override
    public Object resolve(AutomationCondition condition, ExecutionContext context) {
        CustomField field = condition.getCustomField();
        if (field == null) {
            // Not a silent false: a condition on "asset.cf" without a field is a broken rule,
            // and the run log should say so instead of reporting "condition not met".
            throw new CustomException("Condition on " + SUBJECT + " has no custom field",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        assertSameCompany(field, context);

        if (!(context.getTriggerEntity() instanceof Asset asset)) {
            return null;
        }
        return context.cached(SUBJECT + "." + field.getId(), () -> asset.getCustomFieldValues().stream()
                .filter(value -> value.getCustomField() != null
                        && value.getCustomField().getId().equals(field.getId()))
                .map(CustomFieldValue::getValue)
                .findFirst()
                .orElse(null));
    }

    /**
     * A rule may only read fields of its own company. Reading a foreign field would not leak
     * anything by itself — the asset would have no value for it and the condition would just be
     * false — but a rule that can never match is worth an error rather than a shrug.
     */
    private void assertSameCompany(CustomField field, ExecutionContext context) {
        Long fieldCompanyId = field.getCompanySettings() == null
                || field.getCompanySettings().getCompany() == null
                ? null
                : field.getCompanySettings().getCompany().getId();
        if (fieldCompanyId == null || !fieldCompanyId.equals(context.getCompany().getId())) {
            throw new CustomException("Custom field " + field.getId() + " does not belong to company "
                    + context.getCompany().getId(), HttpStatus.FORBIDDEN);
        }
    }
}
