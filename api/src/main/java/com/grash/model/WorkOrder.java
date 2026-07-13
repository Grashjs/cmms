package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.Status;
import com.grash.utils.Helper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
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
public class WorkOrder extends WorkOrderBase {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @Audited(withModifiedFlag = true)
    private String customId;

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private OwnUser completedBy;

    private Date completedOn;

    private Status status = Status.OPEN;

    @OneToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private File signature;

    private boolean archived;

    @ManyToOne
    @JsonIgnore
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private Request parentRequest;

    private String feedback;


    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private PreventiveMaintenance parentPreventiveMaintenance;

    @NotAudited
    private Date firstTimeToReact;


    public boolean isAssignedTo(OwnUser user) {
        Collection<OwnUser> users = getUsers();
        return users.stream().anyMatch(user1 -> user1.getId().equals(user.getId()));
    }

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
    public List<OwnUser> getNewUsersToNotify(Collection<OwnUser> newUsers) {
        Collection<OwnUser> oldUsers = getUsers();
        return newUsers.stream().filter(newUser -> oldUsers.stream().noneMatch(user -> user.getId().equals(newUser.getId()))).
                collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparingLong(OwnUser::getId))),
                        ArrayList::new));
    }

    public boolean canBeEditedBy(OwnUser user) {
        return user.getRole().getEditOtherPermissions().contains(PermissionEntity.WORK_ORDERS)
                || (this.getCreatedBy() != null && this.getCreatedBy().equals(user.getId())) || isAssignedTo(user);
    }

    //in days
    @JsonIgnore
    public static long getAverageAge(Collection<WorkOrder> completeWorkOrders) {
        List<Long> completionTimes = completeWorkOrders.stream().map(workOrder ->
                        Helper.getDateDiff(workOrder.getParentRequest() == null ? workOrder.getCreatedAt() :
                                workOrder.getParentRequest().getCreatedAt(), workOrder.getCompletedOn(), TimeUnit.DAYS))
                .collect(Collectors.toList());
        return completionTimes.isEmpty() ? 0 :
                completionTimes.stream().mapToLong(value -> value).sum() / completionTimes.size();
    }
}
