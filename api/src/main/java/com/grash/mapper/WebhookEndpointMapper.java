package com.grash.mapper;

import com.grash.dto.webhookEndpoint.WebhookEndpointPatchDTO;
import com.grash.dto.webhookEndpoint.WebhookEndpointPostDTO;
import com.grash.dto.webhookEndpoint.WebhookEndpointShowDTO;
import com.grash.model.WebhookEndpoint;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface WebhookEndpointMapper {
    WebhookEndpoint updateWebhookEndpoint(@MappingTarget WebhookEndpoint entity,
                                                        WebhookEndpointPatchDTO dto);

    WebhookEndpoint fromPostDto(@Valid WebhookEndpointPostDTO dto);

    WebhookEndpointShowDTO toShowDto(@Valid WebhookEndpoint model);
}
