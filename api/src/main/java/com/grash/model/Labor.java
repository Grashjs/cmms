package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.abstracts.Time;
import com.grash.model.enums.TimeStatus;
import com.grash.utils.Helper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor
public class Labor extends Time {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private OwnUser assignedTo;

    private boolean includeToTotalTime = true;

    private boolean logged = false;

    private long hourlyRate;

    private boolean isDemo;

    @NotNull
    private Date startedAt;

    private TimeStatus status = TimeStatus.STOPPED;

    @ManyToOne(fetch = FetchType.LAZY)
    private TimeCategory timeCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull
    private WorkOrder workOrder;

    public long getCost() {
        return hourlyRate * this.getDuration() / 3600;
    }

    public Date getEndedAt() {
        return Helper.addSeconds(startedAt, Math.toIntExact(this.getDuration()));
    }

    public Labor(OwnUser user, long hourlyRate, Date startedAt, WorkOrder workOrder, boolean logged,
                 TimeStatus status) {
        this.assignedTo = user;
        this.hourlyRate = hourlyRate;
        this.startedAt = startedAt;
        this.workOrder = workOrder;
        this.status = status;
        this.logged = logged;
    }

    public static long getTotalWorkDuration(List<Labor> labors) {
        // Sort labors by their start time
        labors = labors.stream().filter(labor -> labor.getStartedAt() != null).collect(Collectors.toList());
        labors.sort(Comparator.comparing(Labor::getStartedAt));

        long totalDuration = 0;
        Date previousEnd = null;

        for (Labor labor : labors) {
            Date currentStart = labor.getStartedAt();
            Date currentEnd = labor.getEndedAt();

            // If there is an overlap with the previous labor
            if (previousEnd != null && currentStart.before(previousEnd)) {
                // Calculate the duration considering overlap
                totalDuration += (currentEnd.getTime() - previousEnd.getTime()) / 1000;
            } else {
                // No overlap, add the duration of the current labor
                totalDuration += labor.getDuration();
            }

            // Update previousEnd for the next iteration
            previousEnd = currentEnd;
        }

        return totalDuration;
    }
}
