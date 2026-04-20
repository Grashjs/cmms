package com.grash.mapper;

import com.grash.dto.apiKey.ApiKeyPatchDTO;
import com.grash.dto.apiKey.ApiKeyPostDTO;
import com.grash.dto.apiKey.ApiKeyShowDTO;
import com.grash.model.ApiKey;
import jakarta.validation.Valid;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ApiKeyMapper {
    ApiKey updateApiKey(@MappingTarget ApiKey entity,
                        ApiKeyPatchDTO dto);

    ApiKey fromPostDto(@Valid ApiKeyPostDTO dto);

    ApiKeyShowDTO toShowDto(@Valid ApiKey model);

    @AfterMapping
    default void hideCode(@MappingTarget ApiKeyShowDTO target, ApiKey model) {
        String code = model.getCode();

        String last4 = code.substring(code.length() - 4);
        String masked = "*".repeat(5) + last4;
        target.setCode(masked);
    }
}
