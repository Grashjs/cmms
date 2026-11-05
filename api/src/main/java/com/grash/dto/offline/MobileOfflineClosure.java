package com.grash.dto.offline;

import java.util.List;

/** Existing closure requirements and read-only presence, without offline stock or cost editors. */
public record MobileOfflineClosure(List<String> requiredFields, boolean askFeedback, boolean requiredSignature,
                                    boolean canChangeStatus, boolean canCaptureSignature,
                                    boolean hasParts, boolean hasCosts) {}
