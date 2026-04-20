package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.dto.DateRange;
import com.grash.model.abstracts.CompanyAudit;
import com.grash.utils.Helper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Schema(description = "Asset downtime entity tracking equipment downtime periods")
public class AssetDowntime extends CompanyAudit {

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Asset asset;

    @Schema(description = "Duration of the downtime in seconds")
    private long duration = 0;

    @Schema(description = "Start date and time of the downtime")
    private Date startsOn;

    public Date getEndsOn() {
        return Helper.addSeconds(startsOn, Math.toIntExact(duration));
    }

    public long getDateRangeDuration(DateRange dateRange) {
        Date start = new Date(Math.max(startsOn.getTime(), dateRange.getStart().getTime()));
        Date end = new Date(Math.min(getEndsOn().getTime(), dateRange.getEnd().getTime()));
        return Helper.getDateDiff(start, end, TimeUnit.SECONDS);
    }

}


