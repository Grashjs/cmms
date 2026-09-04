package com.grash.automation.eval;

import com.grash.automation.model.AutomationCondition;
import com.grash.exception.CustomException;
import com.grash.model.Asset;
import com.grash.model.CustomField;
import com.grash.model.CustomFieldValue;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

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
public class CustomFieldResolver implements OperandResolver {

    public static final String SUBJECT = "asset.cf";

    @Override
    public boolean supports(String subject) {
        return SUBJECT.equals(subject);
    }

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
