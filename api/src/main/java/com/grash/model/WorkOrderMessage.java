package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.MessageType;
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
@Table(name = "work_order_message", indexes = {
        @Index(name = "idx_wo_message_work_order_id", columnList = "work_order_id"),
        @Index(name = "idx_wo_message_created_at", columnList = "createdAt")
})
public class WorkOrderMessage extends CompanyAudit {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    @NotNull
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private OwnUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private MessageType messageType = MessageType.TEXT;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id")
    private WorkOrderMessage parentMessage;

    @Column(nullable = false)
    private boolean edited = false;

    @Column(nullable = false)
    private boolean deleted = false;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkOrderMessageRead> reads = new ArrayList<>();

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkOrderMessageReaction> reactions = new ArrayList<>();

    public WorkOrderMessage(WorkOrder workOrder, OwnUser user, MessageType messageType, String content, File file) {
        this.workOrder = workOrder;
        this.user = user;
        this.messageType = messageType;
        this.content = content;
        this.file = file;
    }
}
