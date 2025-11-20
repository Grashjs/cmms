package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.dto.DateRange;
import com.grash.model.abstracts.CompanyAudit;
import com.grash.utils.Helper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.FetchType;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AssetDowntime extends CompanyAudit {

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Asset asset;

    //seconds can be equal to 0 if created by triggerDowntime
    private long duration = 0;

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
