package com.grash.dto.offline;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record MobileOfflineReceiptRequest(@NotNull @Size(min = 1, max = 100)
                                           List<@NotNull UUID> operationIds) {}
