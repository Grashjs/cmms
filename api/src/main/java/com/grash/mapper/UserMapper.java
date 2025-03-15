package com.grash.mapper;

import com.grash.dto.*;
import com.grash.model.Asset;
import com.grash.model.OwnUser;
import com.grash.model.UiConfiguration;
import com.grash.service.AssetService;
import com.grash.service.UiConfigurationService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Mapper(componentModel = "spring", uses = {SuperAccountRelationMapper.class, FileMapper.class})
public abstract class UserMapper {
    public abstract OwnUser updateUser(@MappingTarget OwnUser entity, UserPatchDTO dto);

    @Lazy
    @Autowired
    private UiConfigurationService uiConfigurationService;

    @Mappings({@Mapping(source = "company.id", target = "companyId"),
            @Mapping(source = "company.companySettings.id", target = "companySettingsId"),
            @Mapping(source = "userSettings.id", target = "userSettingsId")})
    @Mapping(source = "company.companySettings.uiConfiguration", target = "uiConfiguration")
    public abstract UserResponseDTO toResponseDto(OwnUser model);

    @AfterMapping
    protected UserResponseDTO toResponseDto(OwnUser model, @MappingTarget UserResponseDTO target) {
        if (target.getUiConfiguration() == null) {
            UiConfiguration uiConfiguration = new UiConfiguration();
            uiConfiguration.setCompanySettings(model.getCompany().getCompanySettings());
            target.setUiConfiguration(uiConfigurationService.create(uiConfiguration));
        }
        return target;
    }

    @Mappings({})
    public abstract OwnUser toModel(UserSignupRequest dto);

    public abstract UserMiniDTO toMiniDto(OwnUser user);
}
