package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.FileType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class File extends CompanyAudit {
    @NotNull
    private String name;

    @NotNull
    private String path;


    private FileType type = FileType.OTHER;

    private boolean hidden = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Task task;

    @ManyToMany
    @JsonIgnore
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

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_WorkOrder_File_Associations",
            joinColumns = @JoinColumn(name = "id_file"),
            inverseJoinColumns = @JoinColumn(name = "id_work_order"),
            indexes = {
                    @Index(name = "idx_file_work_order_file_id", columnList = "id_file"),
                    @Index(name = "idx_file_work_order_work_order_id", columnList = "id_work_order")
            })
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
}
