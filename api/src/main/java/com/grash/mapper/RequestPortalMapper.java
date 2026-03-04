package com.grash.mapper;

import com.grash.dto.FileShowDTO;
import com.grash.dto.requestPortal.RequestPortalPatchDTO;
import com.grash.dto.requestPortal.RequestPortalPostDTO;
import com.grash.dto.requestPortal.RequestPortalPublicDTO;
import com.grash.dto.requestPortal.RequestPortalShowDTO;
import com.grash.factory.StorageServiceFactory;
import com.grash.model.File;
import com.grash.model.RequestPortal;
import jakarta.validation.Valid;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Mapper(componentModel = "spring")
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
    public abstract RequestPortalPublicDTO toPublicDto(@Valid RequestPortal model);

    @AfterMapping
    protected void toPublicDto(RequestPortal model, @MappingTarget RequestPortalPublicDTO target) {
        if (model.getCompany().getLogo() != null)
            target.setCompanyLogo(storageServiceFactory.getStorageService().generateSignedUrl(model.getCompany().getLogo(), 15));
    }
}
