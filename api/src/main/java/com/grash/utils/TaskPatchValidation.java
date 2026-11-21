package com.grash.utils;

import com.grash.dto.TaskPatchDTO;
import com.grash.model.Task;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;

/** Shared by connected and offline writes. Only explicitly supplied fields are checked. */
public final class TaskPatchValidation {
    private static final Set<String> SUBTASK = Set.of("OPEN", "IN_PROGRESS", "ON_HOLD", "COMPLETE");
    private static final Set<String> INSPECTION = Set.of("PASS", "FLAG", "FAIL");
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?");

    private TaskPatchValidation() {}

    public static String error(Task task, TaskPatchDTO patch) {
        if (patch.hasNotes() && patch.getNotes() != null && patch.getNotes().length() > 255) return "NOTES_TOO_LONG";
        if (!patch.hasValue()) return null;
        String value = patch.getValue();
        if (value != null && value.length() > 255) return "VALUE_TOO_LONG";
        if (task.getTaskBase().getTaskType() == null) return "TYPE_NOT_SUPPORTED";
        return switch (task.getTaskBase().getTaskType()) {
            case TEXT -> null;
            case NUMBER, METER -> finiteDecimal(value) ? null : "INVALID_NUMBER";
            case SUBTASK -> value != null && SUBTASK.contains(value) ? null : "INVALID_TASK_OPTION";
            case INSPECTION -> value != null && INSPECTION.contains(value) ? null : "INVALID_TASK_OPTION";
            case MULTIPLE -> value != null && task.getTaskBase().getOptions().stream()
                    .anyMatch(option -> value.equals(option.getLabel())) ? null : "INVALID_TASK_OPTION";
        };
    }

    private static boolean finiteDecimal(String value) {
        if (value == null || !DECIMAL.matcher(value).matches()) return false;
        try {
            // Match the finite numeric domain used by existing numeric workflow conditions.
            return Double.isFinite(new BigDecimal(value).doubleValue());
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
