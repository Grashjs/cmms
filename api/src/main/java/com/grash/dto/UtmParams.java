package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "UTM tracking parameters for marketing attribution")
public class UtmParams {
    @Schema(description = "UTM source parameter identifying the traffic source")
    private String utm_source;
    @Schema(description = "UTM medium parameter identifying the marketing medium")
    private String utm_medium;
    @Schema(description = "UTM campaign parameter identifying the specific campaign")
    private String utm_campaign;
    @Schema(description = "UTM term parameter identifying paid search terms")
    private String utm_term;
    @Schema(description = "UTM content parameter identifying the specific content")
    private String utm_content;
    @Schema(description = "Google Click Identifier")
    private String gclid;
    @Schema(description = "Facebook Click Identifier")
    private String fbclid;
    @Schema(description = "Referrer URL that directed the user to this page")
    private String referrer;

    public boolean hasAnyParam() {
        return isNotEmpty(utm_source) ||
                isNotEmpty(utm_medium) ||
                isNotEmpty(utm_campaign) ||
                isNotEmpty(utm_term) ||
                isNotEmpty(utm_content) ||
                isNotEmpty(gclid) ||
                isNotEmpty(fbclid) ||
                isNotEmpty(referrer);
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

}
