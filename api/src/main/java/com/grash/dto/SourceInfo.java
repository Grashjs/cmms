package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

// Modified by the Fierabrás CMMS fork, 2026-08 — AGPLv3 corresponding source offer.

@Data
@Builder
@Schema(description = "AGPLv3 corresponding source offer endpoint")
public class SourceInfo {
    @Schema(description = "SPDX license identifier", example = "AGPL-3.0-only")
    private String license;
    @Schema(description = "Public repository URL of the modified version")
    private String sourceCodeUrl;
    @Schema(description = "Exact deployed revision (commit or tag)")
    private String revision;
    @Schema(description = "Direct URL to the exact deployed source tree")
    private String correspondingSourceUrl;
}