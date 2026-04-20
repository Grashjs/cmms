package com.grash.model.abstracts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.dto.IdDTO;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.Priority;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.http.HttpStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.*;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Data
@MappedSuperclass
public abstract class WorkOrderBase extends CompanyAudit {

    @Schema(description = "The due date for completing the work order")
    private Date dueDate;

    @Schema(description = "The priority level of the work order")
    private Priority priority = Priority.NONE;

    @Schema(description = "The estimated duration to complete the work order (in hours)")
    private double estimatedDuration;

    @Schema(description = "The estimated start date for the work order")
    private Date estimatedStartDate;

    @Schema(description = "Detailed description of the work order", maxLength = 10000)
    @Column(length = 10000)
    private String description;

    @Schema(description = "The title of the work order", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String title;

    @Schema(description = "Indicates whether a signature is required upon completion")
    private boolean requiredSignature;

    @Schema(description = "Image file associated with the work order", implementation = IdDTO.class)
    @OneToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private File image;

    @Schema(description = "The category of the work order", implementation = IdDTO.class)
    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private WorkOrderCategory category;

    @Schema(description = "The location where the work is to be performed", implementation = IdDTO.class)
    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private Location location;

    @Schema(description = "The team assigned to perform the work", implementation = IdDTO.class)
    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private Team team;

    @Schema(description = "The primary user responsible for the work order", implementation = IdDTO.class)
    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private User primaryUser;

    @ManyToMany
    @NotAudited
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of users assigned to the work order", writeOnly = true)
    )
    private List<User> assignedTo = new ArrayList<>();

    @ManyToMany
    @NotAudited
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of customers associated with the work order")
    )
    private List<Customer> customers = new ArrayList<>();

    @ManyToMany
    @NotAudited
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of files attached to the work order")
    )
    private List<File> files = new ArrayList<>();

    @Schema(description = "The asset on which the work order is created", implementation = IdDTO.class)
    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, withModifiedFlag = true)
    private Asset asset;

    @JsonIgnore
    public Collection<User> getUsers() {
        Collection<User> users = new ArrayList<>();
        if (this.getPrimaryUser() != null) {
            users.add(this.getPrimaryUser());
        }
        if (this.getTeam() != null) {
            users.addAll(this.getTeam().getUsers());
        }
        if (this.getAssignedTo() != null) {
            users.addAll(this.getAssignedTo());
        }
        return users.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparingLong(User::getId))),
                ArrayList::new));
    }

    public void setEstimatedDuration(double estimatedDuration) {
        if (estimatedDuration < 0)
            throw new CustomException("Estimated duration should not be negative", HttpStatus.NOT_ACCEPTABLE);
        this.estimatedDuration = estimatedDuration;
    }

    public boolean isAssignedTo(User user) {
        Collection<User> users = getUsers();
        return users.stream().anyMatch(user1 -> user1.getId().equals(user.getId()));
    }
}


