package com.grash.dto.offline;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MobileOfflinePreparationRequest(@NotNull UUID deviceId) {}
