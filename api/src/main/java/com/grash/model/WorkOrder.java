package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.dto.IdDTO;
import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.Status;
import com.grash.utils.Helper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import jakarta.persistence.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Audited(withModifiedFlag = true)
@AuditOverride(forClass = WorkOrderBase.class)
@Schema(description = "Work order entity representing a maintenance or repair task")
public class WorkOrder extends WorkOrderBase {

    @Schema(description = "Unique identifier of the work order", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Schema(description = "Custom identifier for the work order", accessMode = Schema.AccessMode.READ_ONLY)
    @Audited(withModifiedFlag = true)
    private String customId;

    @Schema(description = "The user who completed the work order", implementation = IdDTO.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private User completedBy;

    @Schema(description = "The date and time when the work order was completed")
    private Date completedOn;

    @Schema(description = "The current status of the work order")
    private Status status = Status.OPEN;

    @Schema(description = "Signature captured upon work order completion")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private String signature;

    @Schema(description = "Indicates whether the work order is archived")
    private boolean archived;

    @Schema(description = "Indicates whether this is a demo work order")
    private boolean isDemo;

    @Schema(description = "The parent request from which this work order was created", implementation = IdDTO.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private Request parentRequest;

    @Schema(description = "Feedback provided upon work order completion")
    private String feedback;


    @Schema(description = "The preventive maintenance schedule that generated this work order", implementation =
            IdDTO.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private PreventiveMaintenance parentPreventiveMaintenance;

    @Schema(description = "The first time the work order was reacted to", accessMode = Schema.AccessMode.READ_ONLY)
    @NotAudited
    private Date firstTimeToReact;

    @NotAudited
    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomFieldValue> customFieldValues = new ArrayList<>();

    @JsonIgnore
    public boolean isCompliant() {
        return this.getDueDate() == null || this.getCompletedOn().before(this.getDueDate());
    }

    @JsonIgnore
    public boolean isReactive() {
        return this.getParentPreventiveMaintenance() == null;
    }

    @JsonIgnore
    public Date getRealCreatedAt() {
        return this.getParentRequest() == null ? this.getCreatedAt() : this.getParentRequest().getCreatedAt();
    }

    @JsonIgnore
    public List<User> getNewUsersToNotify(Collection<User> newUsers) {
        Collection<User> oldUsers = getUsers();
        return newUsers.stream().filter(newUser -> oldUsers.stream().noneMatch(user -> user.getId().equals(newUser.getId()))).
                collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparingLong(User::getId))),
                        ArrayList::new));
    }

    public boolean canBeEditedBy(User user) {
        return user.getRole().getEditOtherPermissions().contains(PermissionEntity.WORK_ORDERS)
                || (this.getCreatedBy() != null && this.getCreatedBy().equals(user.getId())) || isAssignedTo(user);
    }

    //in days
    @JsonIgnore
    public static long getAverageAge(Collection<WorkOrder> completeWorkOrders) {
        List<Long> completionTimes = completeWorkOrders.stream()
                .filter(workOrder -> workOrder.getCompletedOn() != null).map(workOrder ->
                        Helper.getDateDiff(workOrder.getParentRequest() == null ? workOrder.getCreatedAt() :
                                workOrder.getParentRequest().getCreatedAt(), workOrder.getCompletedOn(), TimeUnit.DAYS))
                .collect(Collectors.toList());
        return completionTimes.isEmpty() ? 0 :
                completionTimes.stream().mapToLong(value -> value).sum() / completionTimes.size();
    }

    public boolean isAccessibleBy(User user) {
        return (user.getRole().getViewPermissions().contains(PermissionEntity.WORK_ORDERS) &&
                (user.getRole().getViewOtherPermissions().contains(PermissionEntity.WORK_ORDERS) || (getCreatedBy() != null && getCreatedBy().equals(user.getId())) || isAssignedTo(user)))
                || getParentRequest() != null && getParentRequest().getCreatedBy().equals(user.getId())
                ;
    }
}

