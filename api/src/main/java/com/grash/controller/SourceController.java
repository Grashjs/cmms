package com.grash.controller;

import com.grash.dto.SourceInfo;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Modified by the Fierabrás CMMS fork, 2026-08 — AGPLv3 corresponding source offer.
// Public, unauthenticated endpoint required by AGPLv3 section 13 for users
// interacting with the modified version remotely. See NOTICE.md.

@RestController
@RequestMapping("/source")
@RequiredArgsConstructor
public class SourceController {

    @Value("${source.code-url:#{null}}")
    private String sourceCodeUrl;

    @Value("${source.revision:#{null}}")
    private String sourceRevision;

    @Operation(summary = "AGPLv3 corresponding source offer")
    @GetMapping
    public SourceInfo getSourceInfo() {
        String url = sourceCodeUrl == null ? "" : sourceCodeUrl;
        String revision = sourceRevision == null ? "" : sourceRevision;
        String corresponding = "";
        if (!url.isEmpty() && !revision.isEmpty()) {
            String base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            corresponding = base + "/tree/" + revision;
        }
        return SourceInfo.builder()
                .license("AGPL-3.0-only")
                .sourceCodeUrl(url)
                .revision(revision)
                .correspondingSourceUrl(corresponding)
                .build();
    }
}