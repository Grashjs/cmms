package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.FileType;
import com.grash.model.enums.PermissionEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "File entity representing an attachment or document")
public class File extends CompanyAudit {
    @Schema(description = "Name of the file")
    @NotNull
    private String name;

    @Schema(description = "Storage path of the file")
    @NotNull
    private String path;


    @Schema(description = "Type/category of the file")
    private FileType type = FileType.OTHER;

    @Schema(description = "Indicates whether the file is hidden")
    private boolean hidden = false;

    @Schema(description = "Storage path of the thumbnail image")
    private String thumbnailPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Task task;

    // Mirrors Asset.files onto the same table with the columns swapped, so both sides read
    // the rows that Asset.files writes. Batched because FileShowDTO exposes the list and a
    // page of files would otherwise cost one query per row.
    @ManyToMany
    @JsonIgnore
    @BatchSize(size = 64)
    @JoinTable(name = "T_Asset_File_Associations",
            joinColumns = @JoinColumn(name = "id_file"),
            inverseJoinColumns = @JoinColumn(name = "id_asset"),
            indexes = {
                    @Index(name = "idx_file_asset_file_id", columnList = "id_file"),
                    @Index(name = "idx_file_asset_asset_id", columnList = "id_asset")
            })
    private List<Asset> assets = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Part_File_Associations",
            joinColumns = @JoinColumn(name = "id_file"),
            inverseJoinColumns = @JoinColumn(name = "id_part"),
            indexes = {
                    @Index(name = "idx_file_part_file_id", columnList = "id_file"),
                    @Index(name = "idx_file_part_part_id", columnList = "id_part")
            })
    private List<Part> parts = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Request_File_Associations",
            joinColumns = @JoinColumn(name = "id_file"),
            inverseJoinColumns = @JoinColumn(name = "id_request"),
            indexes = {
                    @Index(name = "idx_file_request_file_id", columnList = "id_file"),
                    @Index(name = "idx_file_request_request_id", columnList = "id_request")
            })
    private List<Request> Requests = new ArrayList<>();

    // Points at work_order_files, not at T_WorkOrder_File_Associations.
    //
    // Both tables exist, which is why the app boots with ddl-auto: validate, but only one of
    // them is ever written: WorkOrderBase.files declares no @JoinTable, so Hibernate names it
    // work_order_files(work_order_id, files_id), and that is where WorkOrderService puts
    // attachments. This side previously mapped T_WorkOrder_File_Associations, which nothing
    // writes — a filter over it returned an empty page instead of an error, which reads like
    // "no files on this work order".
    //
    // Attachments on preventive maintenances live in preventive_maintenance_files and are
    // deliberately out of scope here: this collection is typed to WorkOrder.
    @ManyToMany
    @JsonIgnore
    @BatchSize(size = 64)
    @JoinTable(name = "work_order_files",
            joinColumns = @JoinColumn(name = "files_id"),
            inverseJoinColumns = @JoinColumn(name = "work_order_id"))
    private List<WorkOrder> workOrders = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Location_File_Associations",
            joinColumns = @JoinColumn(name = "id_file"),
            inverseJoinColumns = @JoinColumn(name = "id_location"),
            indexes = {
                    @Index(name = "idx_file_location_file_id", columnList = "id_file"),
                    @Index(name = "idx_file_location_location_id", columnList = "id_location")
            })
    private List<Location> locations = new ArrayList<>();

    public File(String name, String path, FileType fileType, Task task, boolean hidden) {
        this.name = name;
        this.path = path;
        this.type = fileType;
        this.task = task;
        this.hidden = hidden;
    }

    public boolean canBeEditedBy(User user) {
        return user.getRole().getEditOtherPermissions().contains(PermissionEntity.FILES)
                || (this.getCreatedBy() != null && this.getCreatedBy().equals(user.getId()));
    }

    public boolean canBeDeletedBy(User user) {
        return user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.FILES)
                || (this.getCreatedBy() != null && this.getCreatedBy().equals(user.getId()));
    }

    public boolean canBeViewedBy(User user) {
        return (user.getRole().getViewPermissions().contains(PermissionEntity.FILES) &&
                (user.getRole().getViewOtherPermissions().contains(PermissionEntity.FILES) || (getCreatedBy() != null && getCreatedBy().equals(user.getId()))));
    }
}


