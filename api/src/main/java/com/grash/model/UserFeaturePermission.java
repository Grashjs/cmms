package com.grash.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_feature_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFeaturePermission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private OwnUser user;
    
    @ManyToOne
    @JoinColumn(name = "feature_code", referencedColumnName = "code", nullable = false)
    private Feature feature;
    
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;
    
    @ManyToOne
    @JoinColumn(name = "granted_by")
    private OwnUser grantedBy;
    
    @Column(name = "granted_at")
    private LocalDateTime grantedAt = LocalDateTime.now();
    
    @Column(columnDefinition = "TEXT")
    private String notes;
}
