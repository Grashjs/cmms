package com.grash.dto.triage;

import com.grash.dto.AssetMiniDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "One asset proposed by triage, with the words that produced the match")
public class QualificationCandidateShowDTO {

    @Schema(description = "The proposed asset")
    private AssetMiniDTO asset;

    /**
     * Named a match rather than a confidence, and the difference is not cosmetic. It says how
     * much of the request text this asset accounts for, and it is comparable only against other
     * candidates from the same engine - not against a probability, and not across engines.
     */
    @Schema(description = "Match strength between 0 and 1, comparable only within the same engine")
    private double score;

    @Schema(description = "Position in the ranking, 0 is the best candidate")
    private int ordinal;

    /**
     * Split back into a list here rather than handed over as the stored string: the frontend
     * shows one chip per word, and a comma-separated string would have it splitting on a
     * separator the backend chose.
     */
    @Schema(description = "The words that produced the match")
    private List<String> matchedTerms;
}
