package com.grash.automation.action;

import com.grash.automation.eval.ExecutionContext;
import com.grash.automation.model.ActionType;
import com.grash.automation.model.AutomationActionStep;
import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.exception.CustomException;
import com.grash.model.Asset;
import com.grash.model.CustomFieldValue;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.service.AssetService;
import com.grash.service.CustomFieldValueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes one custom field value on the triggering asset.
 *
 * <p>This handler is what makes custom fields symmetric — the engine can already read them as
 * conditions, and without writing them it could never advance a state it evaluates itself. That
 * symmetry, not any single workflow, is what "flexibly extensible" means here.
 *
 * <p>Parameters: {@code customField} (id) and {@code value}.
 *
 * <p>Two things make this harder than it looks:
 *
 * <ul>
 *   <li>{@code CustomFieldValueService.setCustomFields} <b>replaces the whole set</b> — it calls
 *       {@code clear()} first. Passing only the one field would therefore delete every other
 *       value on the asset. So the full list is rebuilt with one entry changed. The alternative,
 *       writing the row directly, would duplicate the category-binding rules and is exactly the
 *       kind of second implementation that drifts.</li>
 *   <li>A value whose field is not bound to the asset's category is <b>discarded, not
 *       refused</b> (see {@code docs/custom-field-categories.md}). Silence would make a rule look
 *       successful while writing nothing, so the write is verified afterwards and the step fails
 *       if the value did not land.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SetCustomFieldHandler implements ActionHandler {

    private final CustomFieldValueService customFieldValueService;
    private final AssetService assetService;

    @Override
    public ActionType getType() {
        return ActionType.SET_CUSTOM_FIELD;
    }

    @Override
    public ActionDescriptor descriptor() {
        return new ActionDescriptor(ActionType.SET_CUSTOM_FIELD, "automation_action_set_custom_field",
                List.of(
                        ActionDescriptor.Parameter.entity("customField", "CUSTOM_FIELD", true),
                        // TEXT even for a choice field: which options are permitted depends on
                        // the field chosen in the parameter above, so it is the editor that pairs
                        // the two using the operand metadata, not this static list.
                        ActionDescriptor.Parameter.text("value", true)));
    }

    @Override
    public void execute(AutomationActionStep step, ExecutionContext context) {
        ActionParameters parameters = ActionParameters.of(step.getParameters(), context);
        Long fieldId = parameters.requireLong("customField");
        String value = parameters.getString("value");

        if (!(context.getTriggerEntity() instanceof Asset asset)) {
            throw new CustomException("SET_CUSTOM_FIELD only applies to assets, but this rule was "
                    + "triggered by " + context.getEvent().entityType(), HttpStatus.UNPROCESSABLE_ENTITY);
        }

        List<CustomFieldValuePostDTO> merged = mergeWithExisting(asset, fieldId, value);
        Long categoryId = asset.getCategory() == null ? null : asset.getCategory().getId();

        customFieldValueService.setCustomFields(
                asset,
                asset.getCustomFieldValues(),
                merged,
                context.getCompany(),
                CustomFieldEntityType.ASSET,
                customFieldValue -> customFieldValue.setAsset(asset),
                categoryId);

        assertLanded(asset, fieldId, value, categoryId);
        assetService.save(asset);
    }

    /**
     * The existing values plus the new one, so replacing the whole set keeps everything else.
     * An existing entry for the same field is overwritten rather than added twice.
     */
    private List<CustomFieldValuePostDTO> mergeWithExisting(Asset asset, Long fieldId, String value) {
        List<CustomFieldValuePostDTO> merged = new ArrayList<>();
        boolean replaced = false;
        for (CustomFieldValue existing : asset.getCustomFieldValues()) {
            if (existing.getCustomField() == null) {
                continue;
            }
            CustomFieldValuePostDTO dto = new CustomFieldValuePostDTO();
            dto.setId(existing.getCustomField().getId());
            boolean isTarget = existing.getCustomField().getId().equals(fieldId);
            dto.setValue(isTarget ? value : existing.getValue());
            replaced |= isTarget;
            merged.add(dto);
        }
        if (!replaced) {
            CustomFieldValuePostDTO dto = new CustomFieldValuePostDTO();
            dto.setId(fieldId);
            dto.setValue(value);
            merged.add(dto);
        }
        return merged;
    }

    private void assertLanded(Asset asset, Long fieldId, String value, Long categoryId) {
        boolean landed = asset.getCustomFieldValues().stream()
                .anyMatch(customFieldValue -> customFieldValue.getCustomField() != null
                        && customFieldValue.getCustomField().getId().equals(fieldId)
                        && java.util.Objects.equals(customFieldValue.getValue(), value));
        if (!landed) {
            throw new CustomException("Custom field " + fieldId + " was not written on asset "
                    + asset.getId() + ". It is most likely bound to other asset categories than "
                    + categoryId + ", in which case the value is dropped rather than refused.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
