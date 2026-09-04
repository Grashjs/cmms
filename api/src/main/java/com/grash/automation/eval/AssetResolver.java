package com.grash.automation.eval;

import com.grash.automation.model.AutomationCondition;
import com.grash.model.Asset;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Reads the asset's own fields. Native fields only — custom fields are
 * {@link CustomFieldResolver}'s job, because they are stored elsewhere and carry a category
 * binding this resolver knows nothing about.
 */
@Component
public class AssetResolver implements OperandResolver {

    private static final Set<String> SUBJECTS = Set.of(
            "asset.status", "asset.name", "asset.category", "asset.location", "asset.primaryUser");

    @Override
    public boolean supports(String subject) {
        return SUBJECTS.contains(subject);
    }

    @Override
    public Object resolve(AutomationCondition condition, ExecutionContext context) {
        if (!(context.getTriggerEntity() instanceof Asset asset)) {
            return null;
        }
        return context.cached(condition.getSubject(), () -> switch (condition.getSubject()) {
            case "asset.status" -> asset.getStatus() == null ? null : asset.getStatus().name();
            case "asset.name" -> asset.getName();
            // Ids, not names: a rule points at a category, and renaming the category must not
            // change which assets the rule matches.
            case "asset.category" -> asset.getCategory() == null ? null : asset.getCategory().getId();
            case "asset.location" -> asset.getLocation() == null ? null : asset.getLocation().getId();
            case "asset.primaryUser" -> asset.getPrimaryUser() == null ? null : asset.getPrimaryUser().getId();
            default -> null;
        });
    }
}
