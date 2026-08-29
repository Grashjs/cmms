package com.grash.dto.triage;

import com.grash.dto.AssetMiniDTO;
import com.grash.dto.AuditShowDTO;
import com.grash.dto.UserMiniDTO;
import com.grash.model.enums.QualificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Schema(description = "Triage result for a request: ranked asset suggestions and what was done with them")
public class RequestQualificationShowDTO extends AuditShowDTO {

    @Schema(description = "The request this qualification is about")
    private Long requestId;

    @Schema(description = "Whether the suggestion is still open, was applied or was rejected")
    private QualificationStatus status;

    /**
     * Exposed rather than kept internal, because the card in the request view says which engine
     * produced the suggestion. That is not decoration: a user who knows the answer came from word
     * matching reads a wrong suggestion as a limitation rather than as the system being broken.
     */
    @Schema(description = "Matcher and version that produced the candidates, e.g. lexical-v1")
    private String engine;

    @Schema(description = "Proposed assets, best first")
    private List<QualificationCandidateShowDTO> candidates;

    @Schema(description = "The candidate that was applied, if any")
    private AssetMiniDTO chosenAsset;

    @Schema(description = "Who applied or rejected the suggestion")
    private UserMiniDTO decidedBy;

    @Schema(description = "When the suggestion was applied or rejected")
    private Date decidedAt;
}
