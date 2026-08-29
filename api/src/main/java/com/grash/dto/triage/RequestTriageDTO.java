package com.grash.dto.triage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The triage state of one request, which quite often is "there is none".
 *
 * <p>An envelope rather than a bare nullable body, and the reason is the client rather than
 * taste: the frontend fetch helper parses every response as JSON, and an empty body throws
 * there. A 404 for the ordinary case of a request nothing matched would be worse still - it is
 * not an error, it is the expected answer for most requests, and it would fill the console with
 * failures that mean nothing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Triage state of a request; the qualification is null when there is no suggestion")
public class RequestTriageDTO {

    @Schema(description = "The suggestion currently on offer, or null if triage had nothing to say")
    private RequestQualificationShowDTO qualification;
}
