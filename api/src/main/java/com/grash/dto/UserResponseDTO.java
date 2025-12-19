package com.grash.dto;

import com.grash.model.File;
import com.grash.model.Role;
import com.grash.model.SuperAccountRelation;
import com.grash.model.UiConfiguration;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class UserResponseDTO {

    private Integer id;
    @ApiModelProperty(position = 1)
    private String username;
    @ApiModelProperty(position = 2)
    private String email;
    @ApiModelProperty(position = 3)
    private Role role;

    private long rate;
    private String jobTitle;

    private String firstName;

    private String lastName;

    private String phone;

    private boolean ownsCompany;

    private Long companyId;

    private Long companySettingsId;

    private Long userSettingsId;

    private FileShowDTO image;

    private List<SuperAccountRelationDTO> superAccountRelations = new ArrayList<>();

    private SuperAccountRelationDTO parentSuperAccount;

    private Boolean enabled;

    private Boolean enabledInSubscription;

    private UiConfiguration uiConfiguration;

    private Date lastLogin;

    private Date createdAt;

}
