package com.grash.automation.eval;

import com.grash.automation.model.ConditionOperator;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * What a resolver can read, described well enough for a UI to build the input for it.
 *
 * <p>This is the half of the design that stops the enum mirrors from coming back. The old engine
 * needed every condition spelled out in a Java enum, a switch, and the same list copied into
 * three TypeScript files; a new condition therefore touched five places and drifted between them.
 * Here the editor asks the server what exists and renders whatever it gets, so a new resolver is
 * visible in the UI without a line of frontend work.
 *
 * @param labelKey  an i18n key the frontend translates, for anything the application defines
 * @param label     a literal, for anything the user defined — a custom field's own name, which
 *                  cannot be translated because it is data, not vocabulary
 * @param valueType how to render the input: ENUM and CHOICE come with {@link #options},
 *                  ENTITY_* needs the matching picker, TEXT and NUMBER are plain fields
 * @param options   the only values that can ever match, where that is knowable
 * @param boundToCategories asset category names a custom field is restricted to. Empty means it
 *                  applies everywhere. The editor has to show this: a condition on a bound field
 *                  silently never matches for an asset of another category, which looks exactly
 *                  like a broken rule.
 */
@Schema(description = "A value a condition can be built on")
public record OperandDescriptor(
        String subject,
        Long customFieldId,
        String labelKey,
        String label,
        String valueType,
        List<ConditionOperator> operators,
        List<String> options,
        List<String> boundToCategories) {

    /**
     * A field the application itself defines, so its label is a translation key derived from the
     * subject: {@code asset.status} becomes {@code automation_subject_asset_status}. Derived
     * rather than passed in, because a key spelled out separately is a key that can be spelled
     * wrong, and a missing translation surfaces as the raw key in the editor.
     */
    public static OperandDescriptor native_(String subject, String valueType,
                                            List<ConditionOperator> operators, List<String> options) {
        return new OperandDescriptor(subject, null,
                "automation_subject_" + subject.replace('.', '_'), null, valueType, operators,
                options, List.of());
    }
}
