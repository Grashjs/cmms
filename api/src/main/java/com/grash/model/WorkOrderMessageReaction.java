package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.abstracts.Audit;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@Table(name = "work_order_message_reaction",
        uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id", "reaction"}),
        indexes = {
                @Index(name = "idx_wo_message_reaction_message_id", columnList = "message_id")
        })
public class WorkOrderMessageReaction extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    @NotNull
    private WorkOrderMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private OwnUser user;

    @Column(nullable = false, length = 10)
    @NotNull
    private String reaction;

    public WorkOrderMessageReaction(WorkOrderMessage message, OwnUser user, String reaction) {
        this.message = message;
        this.user = user;
        this.reaction = reaction;
    }
}
