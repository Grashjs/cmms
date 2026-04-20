package com.grash.dto;

import com.grash.model.File;
import com.grash.model.Role;
import com.grash.model.SuperAccountRelation;
import com.grash.model.UiConfiguration;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Schema(description = "DTO containing user details for API responses")
public class UserResponseDTO {

    @Schema(description = "Unique identifier")
    private Integer id;
    
    @Schema(description = "Username")
    private String username;
    
    @Schema(description = "Email address")
    private String email;
    
    @Schema(description = "User role")
    private Role role;

    @Schema(description = "Hourly rate")
    private long rate;
    
    @Schema(description = "Job title")
    private String jobTitle;

    @Schema(description = "First name")
    private String firstName;

    @Schema(description = "Last name")
    private String lastName;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Whether the user owns a company")
    private boolean ownsCompany;

    @Schema(description = "Company ID")
    private Long companyId;

    @Schema(description = "Company settings ID")
    private Long companySettingsId;

    @Schema(description = "User settings ID")
    private Long userSettingsId;

    @Schema(description = "User profile image")
    private FileShowDTO image;

    @Schema(description = "Super account relationships")
    private List<SuperAccountRelationDTO> superAccountRelations = new ArrayList<>();

    @Schema(description = "Parent super account")
    private SuperAccountRelationDTO parentSuperAccount;

    @Schema(description = "Whether the user is enabled")
    private Boolean enabled;

    @Schema(description = "Whether the user is enabled in the subscription")
    private Boolean enabledInSubscription;

    @Schema(description = "UI configuration")
    private UiConfiguration uiConfiguration;

    @Schema(description = "Last login timestamp")
    private Date lastLogin;

    @Schema(description = "Creation timestamp")
    private Date createdAt;

    @Schema(description = "Paddle user ID")
    private String paddleUserId;

}
