package com.grash.controller;

import com.grash.dto.triage.RequestTriageDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.RequestQualificationMapper;
import com.grash.model.Request;
import com.grash.model.User;
import com.grash.security.CurrentUser;
import com.grash.service.RequestQualificationService;
import com.grash.service.RequestService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reading and deciding on triage suggestions.
 *
 * <p>Kept out of {@code RequestController}, which is upstream's file and already 400 lines long -
 * one more feature bolted into it is one more merge conflict on every sync. The only thing this
 * feature adds there is a single published event.
 *
 * <p>No permission of its own. Reading a suggestion takes the right to view the request; acting on
 * one takes the right to edit it, because that is literally what applying does - it writes the
 * asset field. Both checks live in the service and the model, next to the equivalent checks for
 * the request itself.
 */
@RestController
@RequestMapping("/request-qualifications")
@Tag(name = "Request Triage", description = "Asset suggestions for incoming maintenance requests")
@RequiredArgsConstructor
public class RequestQualificationController {

    private final RequestQualificationService requestQualificationService;
    private final RequestQualificationMapper requestQualificationMapper;
    private final RequestService requestService;

    /**
     * The suggestion on offer for a request. Answers with an empty envelope rather than a 404 when
     * there is none: most requests have none, either because the reporter already picked an asset
     * or because nothing in the asset list resembles the text, and neither is an error.
     */
    @GetMapping("/request/{requestId}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public RequestTriageDTO getForRequest(@PathVariable("requestId") Long requestId,
                                          @Parameter(hidden = true) @CurrentUser User user) {
        assertCanView(requestId, user);
        return new RequestTriageDTO(requestQualificationService.findLive(requestId, user)
                .map(requestQualificationMapper::toShowDto)
                .orElse(null));
    }

    /**
     * Takes one of the suggested assets onto the request. The asset is named explicitly rather
     * than defaulting to the top candidate, because picking the second one is a normal outcome
     * and the difference between "took the first" and "took the second" is worth recording.
     */
    @PatchMapping("/{id}/apply")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public RequestTriageDTO apply(@PathVariable("id") Long id,
                                  @Parameter(description = "The suggested asset to accept")
                                  @RequestParam Long assetId,
                                  @Parameter(hidden = true) @CurrentUser User user) {
        return new RequestTriageDTO(requestQualificationMapper.toShowDto(
                requestQualificationService.apply(id, assetId, user)));
    }

    /** Dismisses the suggestion. The row stays, marked rejected; that record is the point. */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public RequestTriageDTO reject(@PathVariable("id") Long id,
                                   @Parameter(hidden = true) @CurrentUser User user) {
        return new RequestTriageDTO(requestQualificationMapper.toShowDto(
                requestQualificationService.reject(id, user)));
    }

    /**
     * Asks for a fresh suggestion. Useful after the description was extended or the asset list was
     * cleaned up, and it is also how the matcher gets tried against requests that predate the
     * feature - there is no backfill job, and for an instance this size there does not need to be.
     */
    @PostMapping("/request/{requestId}/rerun")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public RequestTriageDTO rerun(@PathVariable("requestId") Long requestId,
                                  @Parameter(hidden = true) @CurrentUser User user) {
        return new RequestTriageDTO(requestQualificationService.rerun(requestId, user)
                .map(requestQualificationMapper::toShowDto)
                .orElse(null));
    }

    private void assertCanView(Long requestId, User user) {
        Request request = requestService.findById(requestId)
                .orElseThrow(() -> new CustomException("Request not found", HttpStatus.NOT_FOUND));
        if (!request.canBeViewedBy(user)) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }
    }
}
