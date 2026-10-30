package com.grash.controller.analytics;

import com.grash.dto.DateRange;
import com.grash.exception.CustomException;
import com.grash.model.User;
import com.grash.security.CurrentUser;
import com.grash.service.AnalyticsSummaryService;
import com.grash.utils.Helper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;

@RestController
@RequestMapping("/analytics/summary")
@Tag(name = "Analytics Summary", description = "Maintenance summary report of a period")
@RequiredArgsConstructor
public class AnalyticsSummaryController {

    private final AnalyticsSummaryService analyticsSummaryService;

    // no "produces": it would keep the error handler from answering in JSON
    @PostMapping("/pdf")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Operation(summary = "Maintenance summary of the period as a PDF, compared with the previous period of the " +
            "same length")
    public ResponseEntity<byte[]> getPdf(@Parameter(hidden = true) @CurrentUser User user,
                                         @Parameter(description = "Date range and analytics filters") @RequestBody DateRange dateRange,
                                         @RequestParam(required = false) @Parameter(description = "Filter by specific company") Long companyId) {
        if (!user.canSeeAnalytics()) throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        Long resolvedCompanyId = Helper.resolveCompanyId(user, companyId);
        // rendered before anything is written, so that errors are still returned as JSON
        byte[] pdf = analyticsSummaryService.renderPdf(user, resolvedCompanyId, dateRange);
        String fileName = "maintenance-summary-" + new SimpleDateFormat("yyyy-MM-dd").format(dateRange.getStart())
                + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build()
                        .toString())
                .body(pdf);
    }
}
