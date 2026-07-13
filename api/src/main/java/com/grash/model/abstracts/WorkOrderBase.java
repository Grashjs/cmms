package com.grash.model.abstracts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.Priority;
import lombok.Data;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.http.HttpStatus;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.*;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Data
@MappedSuperclass
public abstract class WorkOrderBase extends CompanyAudit {
    private Date dueDate;

    private Priority priority = Priority.NONE;

    private double estimatedDuration;

    private Date estimatedStartDate;

    @Column(length = 10000)
    private String description;

    @NotNull
    private String title;

    private boolean requiredSignature;

    @OneToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private File image;

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private WorkOrderCategory category;

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private Location location;

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private Team team;

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private OwnUser primaryUser;

    @ManyToMany
    @NotAudited
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<OwnUser> assignedTo = new ArrayList<>();

    @ManyToMany
    @NotAudited
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<Customer> customers = new ArrayList<>();

    @ManyToMany
    @NotAudited
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<File> files = new ArrayList<>();

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private Asset asset;

    @JsonIgnore
    public Collection<OwnUser> getUsers() {
        Collection<OwnUser> users = new ArrayList<>();
        if (this.getPrimaryUser() != null) {
            users.add(this.getPrimaryUser());
        }
        if (this.getTeam() != null) {
            users.addAll(this.getTeam().getUsers());
        }
        if (this.getAssignedTo() != null) {
            users.addAll(this.getAssignedTo());
        }
        return users.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparingLong(OwnUser::getId))),
                ArrayList::new));
    }

    public void setEstimatedDuration(double estimatedDuration) {
        if (estimatedDuration < 0)
            throw new CustomException("Estimated duration should not be negative", HttpStatus.NOT_ACCEPTABLE);
        this.estimatedDuration = estimatedDuration;
    }
}
