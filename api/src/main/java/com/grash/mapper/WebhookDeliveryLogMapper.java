package com.grash.mapper;

import com.grash.dto.webhookEndpoint.WebhookDeliveryLogShowDTO;
import com.grash.model.WebhookDeliveryLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WebhookDeliveryLogMapper {
    @Mapping(source = "webhookEndpoint.id", target = "webhookEndpointId")
    WebhookDeliveryLogShowDTO toShowDto(WebhookDeliveryLog model);
}
