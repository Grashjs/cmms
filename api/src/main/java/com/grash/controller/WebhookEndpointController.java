package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.webhookEndpoint.WebhookEndpointPatchDTO;
import com.grash.dto.webhookEndpoint.WebhookEndpointPostDTO;
import com.grash.dto.webhookEndpoint.WebhookEndpointShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.WebhookEndpointMapper;
import com.grash.model.OwnUser;
import com.grash.model.WebhookEndpoint;
import com.grash.security.CurrentUser;
import com.grash.service.WebhookEndpointService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/webhook-endpoints")
@RequiredArgsConstructor
@Hidden
public class WebhookEndpointController {

    private final WebhookEndpointService webhookEndpointService;
    private final WebhookEndpointMapper webhookEndpointMapper;

    @PostMapping
    public ResponseEntity<WebhookEndpointShowDTO> create(
            @Parameter(hidden = true) @CurrentUser OwnUser user,
            @RequestBody WebhookEndpointPostDTO request) {
        WebhookEndpoint endpoint = webhookEndpointService.create(request, user);
        return ResponseEntity.ok(webhookEndpointMapper.toShowDto(endpoint));
    }

    @GetMapping
    public ResponseEntity<List<WebhookEndpointShowDTO>> listEndpoints(
            @Parameter(hidden = true) @CurrentUser OwnUser user) {

        List<WebhookEndpoint> endpoints = webhookEndpointService
                .getActiveEndpointsByCompany(user.getCompany().getId());

        return ResponseEntity.ok(endpoints.stream()
                .map(webhookEndpointMapper::toShowDto)
                .toList());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WebhookEndpointShowDTO> updateEndpoint(
            @Parameter(hidden = true) @CurrentUser OwnUser user,
            @PathVariable Long id,
            @RequestBody WebhookEndpointPatchDTO request) {

        WebhookEndpoint endpoint = webhookEndpointService.update(id, request, user);
        return ResponseEntity.ok(webhookEndpointMapper.toShowDto(endpoint));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponse> deleteEndpoint(
            @Parameter(hidden = true) @CurrentUser OwnUser user,
            @PathVariable Long id) {
        Optional<WebhookEndpoint> optionalWebhookEndpoint = webhookEndpointService.findById(id);
        if (optionalWebhookEndpoint.isPresent()) {
            webhookEndpointService.delete(id);
            return new ResponseEntity<>(new SuccessResponse(true, "Supprimé avec succès"),
                    HttpStatus.OK);
        } else throw new CustomException("Webhook endpoint not found", HttpStatus.NOT_FOUND);

    }

    @PatchMapping("/{id}/rotate-secret")
    public ResponseEntity<SuccessResponse> rotateSecret(
            @Parameter(hidden = true) @CurrentUser OwnUser user,
            @PathVariable Long id) {

        String newSecret = webhookEndpointService.rotateSecret(
                id,
                user.getCompany().getId()
        );

        return ResponseEntity.ok(new SuccessResponse(true, newSecret));
    }

}