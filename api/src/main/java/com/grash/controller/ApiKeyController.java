package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.apiKey.ApiKeyCriteria;
import com.grash.dto.apiKey.ApiKeyPostDTO;
import com.grash.dto.apiKey.ApiKeyShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.ApiKeyMapper;
import com.grash.model.ApiKey;
import com.grash.model.User;
import com.grash.model.enums.PermissionEntity;
import com.grash.security.CurrentUser;
import com.grash.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api-keys")
@RequiredArgsConstructor
@Hidden
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final ApiKeyMapper apiKeyMapper;

    @PostMapping("/search")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public Page<ApiKeyShowDTO> search(@Parameter(description = "API key search criteria") @RequestBody ApiKeyCriteria apiKeyCriteria,
                                      @Parameter(hidden = true) @CurrentUser User user, Pageable pageable) {
        return apiKeyService.findByCriteria(apiKeyCriteria, pageable, user).map(apiKeyMapper::toShowDto);
    }


    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ApiKeyShowDTO create(@Parameter(description = "API key to create") @RequestBody @Valid ApiKeyPostDTO apiKey,
                                @Parameter(hidden = true) @CurrentUser User user) {
        Pair<ApiKey, String> savedApiKeyPair = apiKeyService.create(apiKey, user);
        ApiKeyShowDTO result = apiKeyMapper.toShowDto(savedApiKeyPair.getFirst());
        result.setCode(savedApiKeyPair.getSecond());
        return result;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ApiKeyShowDTO getById(@PathVariable Long id, @Parameter(hidden = true) @CurrentUser User user) {
        if (!user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS))
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        return apiKeyMapper.toShowDto(apiKeyService.findById(id).orElseThrow(() -> new CustomException("Not found",
                HttpStatus.NOT_FOUND)));
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<SuccessResponse> delete(@PathVariable("id") Long id,
                                                  @Parameter(hidden = true) @CurrentUser User user) {
        if (!user.getRole().getViewPermissions().contains(PermissionEntity.SETTINGS))
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);

        ApiKey savedApiKey =
                apiKeyService.findById(id).orElseThrow(() -> new CustomException("Not found",
                        HttpStatus.NOT_FOUND));
        apiKeyService.delete(id);
        return new ResponseEntity<>(new SuccessResponse(true, "Deleted successfully"), HttpStatus.OK);
    }
}
