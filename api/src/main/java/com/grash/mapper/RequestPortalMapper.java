package com.grash.mapper;

import com.grash.dto.requestPortal.RequestPortalFieldShowDTO;
import com.grash.dto.requestPortal.RequestPortalPatchDTO;
import com.grash.dto.requestPortal.RequestPortalPostDTO;
import com.grash.dto.requestPortal.RequestPortalPublicDTO;
import com.grash.dto.requestPortal.RequestPortalShowDTO;
import com.grash.factory.StorageServiceFactory;
import com.grash.mapper.AssetMapper;
import com.grash.mapper.LocationMapper;
import com.grash.model.RequestPortal;
import com.grash.model.RequestPortalField;
import jakarta.validation.Valid;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Mapper(componentModel = "spring", uses = {AssetMapper.class, LocationMapper.class})
public abstract class RequestPortalMapper {
    @Autowired
    @Lazy
    private StorageServiceFactory storageServiceFactory;

    public abstract RequestPortal updateRequestPortal(@MappingTarget RequestPortal entity,
                                                       RequestPortalPatchDTO dto);

    public abstract RequestPortal fromPostDto(@Valid RequestPortalPostDTO dto);

    public abstract RequestPortalShowDTO toShowDto(@Valid RequestPortal model);

    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "company.name", target = "companyName")
    @Mapping(source = "company.companySettings.generalPreferences.language", target = "companyLanguage")
    public abstract RequestPortalPublicDTO toPublicDto(@Valid RequestPortal model);

    @Mapping(target = "asset", source = "asset")
    @Mapping(target = "location", source = "location")
    public abstract RequestPortalFieldShowDTO toFieldShowDto(RequestPortalField field);

    @AfterMapping
    protected void toPublicDto(RequestPortal model, @MappingTarget RequestPortalPublicDTO target) {
        if (model.getCompany().getLogo() != null)
            target.setCompanyLogo(storageServiceFactory.getStorageService().generateSignedUrl(model.getCompany().getLogo(), 15));
    }
}
