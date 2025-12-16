package com.grash.model.envers;

import com.grash.model.*;
import com.grash.model.enums.Status;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.context.MessageSource;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Locale;

@Entity
@Data
@Table(name = "work_order_aud")
@NoArgsConstructor
public class WorkOrderAud implements Serializable {

    @EmbeddedId
    private WorkOrderAudId workOrderAudId;

    @Column(name = "revtype") //0 create 1 MOD 2 Del
    private Integer revtype;

    @Column(name = "due_date")
    private Date dueDate;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "estimated_duration")
    private Double estimatedDuration;

    @Column(name = "estimated_start_date")
    private Date estimatedStartDate;

    @Column(name = "description")
    private String description;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "required_signature")
    private Boolean requiredSignature;

    @ManyToOne
    @JoinColumn(name = "image_id")
    private File image;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private WorkOrderCategory category;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne
    @JoinColumn(name = "primary_user_id")
    private OwnUser primaryUser;

    @ManyToOne
    @JoinColumn(name = "completed_by_id")
    private OwnUser completedBy;

    @Column(name = "completed_on")
    private Date completedOn;

    @Column(name = "status")
    private Status status;

    private String signature;

    @Column(name = "archived")
    private Boolean archived;

    @ManyToOne
    @JoinColumn(name = "parent_request_id")
    private Request parentRequest;

    @Column(name = "feedback")
    private String feedback;

    @ManyToOne
    @JoinColumn(name = "parent_preventive_maintenance_id")
    private PreventiveMaintenance parentPreventiveMaintenance;

    // Include fields for _MOD columns

    @ManyToOne
    @JoinColumn(name = "asset_id")
    private Asset asset;

    // Include fields for _MOD columns for asset_id
    // Fields for mod columns
    @Column(name = "dueDate_MOD")
    private Boolean dueDateMod;

    @Column(name = "priority_MOD")
    private Boolean priorityMod;

    @Column(name = "estimatedDuration_MOD")
    private Boolean estimatedDurationMod;

    @Column(name = "estimatedStartDate_MOD")
    private Boolean estimatedStartDateMod;

    @Column(name = "description_MOD")
    private Boolean descriptionMod;

    @Column(name = "title_MOD")
    private Boolean titleMod;

    @Column(name = "requiredSignature_MOD")
    private Boolean requiredSignatureMod;

    @Column(name = "image_MOD")
    private Boolean imageIdMod;

    @Column(name = "category_MOD")
    private Boolean categoryIdMod;

    @Column(name = "location_MOD")
    private Boolean locationIdMod;

    @Column(name = "team_MOD")
    private Boolean teamIdMod;

    @Column(name = "primaryUser_MOD")
    private Boolean primaryUserIdMod;

    @Column(name = "completedBy_MOD")
    private Boolean completedByIdMod;

    @Column(name = "completedOn_MOD")
    private Boolean completedOnMod;

    @Column(name = "status_MOD")
    private Boolean statusMod;

    @Column(name = "signature_MOD")
    private Boolean signatureMod;


    @Column(name = "archived_MOD")
    private Boolean archivedMod;

    @Column(name = "parentRequest_MOD")
    private Boolean parentRequestIdMod;

    @Column(name = "feedback_MOD")
    private Boolean feedbackMod;

    @Column(name = "parentPreventiveMaintenance_MOD")
    private Boolean parentPreventiveMaintenanceIdMod;


    @Column(name = "asset_MOD")
    private Boolean assetIdMod;

    public String getSummary(MessageSource messageSource, Locale locale) {
        StringBuilder summary = new StringBuilder();

        if (dueDateMod != null && dueDateMod) {
            summary.append("Due Date: ").append(dueDate).append("\n");
        }
        if (priorityMod != null && priorityMod) {
            summary.append("Priority: ").append(messageSource.getMessage(priority.toString(), null, locale)).append(
                    "\n");
        }
        if (estimatedDurationMod != null && estimatedDurationMod) {
            summary.append("Estimated Duration: ").append(estimatedDuration).append("\n");
        }
        if (descriptionMod != null && descriptionMod) {
            summary.append("Description: ").append(description).append("\n");
        }
        if (titleMod != null && titleMod) {
            summary.append("Title: ").append(title).append("\n");
        }
        if (requiredSignatureMod != null && requiredSignatureMod) {
            summary.append("Required Signature: ").append(getBooleanText(requiredSignature, messageSource, locale)).append("\n");
        }
        if (imageIdMod != null && imageIdMod) {
            summary.append("Image.\n");
        }
        if (categoryIdMod != null && categoryIdMod) {
            summary.append("Category: ").append(category == null ? "N/A" : category.getName()).append("\n");
        }
        if (locationIdMod != null && locationIdMod) {
            summary.append("Location: ").append(location == null ? "N/A" : location.getName()).append("\n");
        }
        if (teamIdMod != null && teamIdMod) {
            summary.append("Team: ").append(team == null ? "N/A" : team.getName()).append("\n");
        }
        if (primaryUserIdMod != null && primaryUserIdMod) {
            summary.append("Primary User: ").append(primaryUser == null ? "N/A" : primaryUser.getFullName()).append(
                    "\n");
        }
        if (completedByIdMod != null && completedByIdMod) {
            summary.append("Completed By: ").append(completedBy == null ? "N/A" : completedBy.getFullName()).append(
                    "\n");
        }
        if (completedOnMod != null && completedOnMod) {
            summary.append("Completed On: ").append(completedOn).append("\n");
        }
        if (statusMod != null && statusMod) {
            summary.append("Status: ").append(messageSource.getMessage(status.toString(), null, locale)).append("\n");
        }
        if (signatureMod != null && signatureMod) {
            summary.append("Signature\n");
        }
        if (archivedMod != null && archivedMod) {
            summary.append("Archived: ").append(getBooleanText(archived, messageSource, locale)).append("\n");
        }
        if (parentRequestIdMod != null && parentRequestIdMod) {
            summary.append("Parent Request: ").append(parentRequest == null ? "N/A" : parentRequest.getTitle()).append("\n");
        }
        if (feedbackMod != null && feedbackMod) {
            summary.append("Feedback: ").append(feedback).append("\n");
        }
        if (parentPreventiveMaintenanceIdMod != null && parentPreventiveMaintenanceIdMod) {
            summary.append("Parent Preventive Maintenance: ").append(parentPreventiveMaintenance == null ? "N/A" :
                    parentPreventiveMaintenance.getName()).append("\n");
        }
        if (assetIdMod != null && assetIdMod) {
            summary.append("Asset: ").append(asset == null ? "N/A" : asset.getName()).append("\n");
        }

        return summary.toString();
    }


    private String getBooleanText(Boolean value, MessageSource messageSource, Locale locale) {
        return value == null ? "" : messageSource.getMessage(value ? "Yes" : "No", null, locale);
    }
}
