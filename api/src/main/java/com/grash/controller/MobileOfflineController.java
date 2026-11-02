package com.grash.controller;

import com.grash.dto.SuccessResponse;
import com.grash.dto.offline.*;
import com.grash.model.User;
import com.grash.service.MobileOfflineService;
import com.grash.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/mobile-offline")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_CLIENT')")
public class MobileOfflineController {
    private final MobileOfflineService offline;
    private final UserService users;
    private final com.fasterxml.jackson.databind.ObjectMapper json;

    private User currentUser(HttpServletRequest request) {
        // Authorization changes must not wait for the normal whoami cache to expire.
        return users.whoami(request, false);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, com.fasterxml.jackson.core.JsonProcessingException.class})
    public ResponseEntity<SuccessResponse> invalidRequest() {
        return ResponseEntity.badRequest().body(new SuccessResponse(false, "INVALID_OFFLINE_REQUEST"));
    }

    @ExceptionHandler(TransientDataAccessException.class)
    public ResponseEntity<SuccessResponse> concurrentRetry() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new SuccessResponse(false, "OFFLINE_RETRY_LATER"));
    }

    @GetMapping("/eligibility")
    public MobileOfflineEligibility eligibility(HttpServletRequest request) {
        return offline.eligibility(currentUser(request));
    }

    @PostMapping("/preparations/{id}")
    public MobileOfflineSnapshot prepare(@PathVariable long id,
                                          @Valid @RequestBody MobileOfflinePreparationRequest body,
                                          HttpServletRequest request) {
        return offline.prepare(id, body.deviceId(), currentUser(request));
    }

    @DeleteMapping("/preparations/{id}")
    public SuccessResponse remove(@PathVariable long id, @RequestParam UUID deviceId, HttpServletRequest request) {
        offline.removePreparation(id, deviceId, currentUser(request));
        return new SuccessResponse(true, "Preparation removed");
    }

    @PostMapping(value = "/files/{transferId}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public MobileOfflineResult upload(@PathVariable UUID transferId, @RequestParam("metadata") String metadata,
                                     @RequestPart("file") org.springframework.web.multipart.MultipartFile file,
                                     HttpServletRequest request) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MobileOfflineLimits.PILOT.snapshotBytes())
            throw new com.grash.exception.CustomException("OPERATION_LIMIT_EXCEEDED", HttpStatus.PAYLOAD_TOO_LARGE);
        return offline.uploadFile(transferId, json.readValue(metadata, MobileOfflineFileUpload.class), file, currentUser(request));
    }

    @GetMapping("/preparations/{workOrderId}/files/{fileId}")
    public MobileOfflineFileDownload download(@PathVariable long workOrderId, @PathVariable long fileId,
                                             HttpServletRequest request) {
        return offline.downloadFile(workOrderId, fileId, currentUser(request));
    }

    @PostMapping("/operations")
    public MobileOfflineResult submit(@Valid @RequestBody MobileOfflineOperation operation, HttpServletRequest request) {
        return offline.submit(operation, currentUser(request));
    }

    @PostMapping("/receipts")
    public MobileOfflineReceipts receipts(@Valid @RequestBody MobileOfflineReceiptRequest body, HttpServletRequest request) {
        return offline.receipts(body.operationIds(), currentUser(request));
    }
}
