package com.grash.model.abstracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.CompanySettings;
import com.grash.model.OwnUser;
import com.grash.security.CustomUserDetail;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;

@Data
@MappedSuperclass
@NoArgsConstructor
public abstract class CategoryAbstract extends Audit {

    @NotNull
    private String name;

    private String description;

    private boolean isDemo;

    @ManyToOne
    @JoinColumn(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private CompanySettings companySettings;

    @PrePersist
    public void beforePersist() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || (authentication.getPrincipal().getClass().equals(String.class) && authentication.getPrincipal().equals("anonymousUser")))
            return;
        OwnUser user = ((CustomUserDetail) authentication.getPrincipal()).getUser();
        CompanySettings companySettings = user.getCompany().getCompanySettings();
        this.setCompanySettings(companySettings);
    }

    public CategoryAbstract(String name, CompanySettings companySettings) {
        this.name = name;
        this.companySettings = companySettings;
    }

}


