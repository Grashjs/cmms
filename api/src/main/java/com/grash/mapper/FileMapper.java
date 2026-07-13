package com.grash.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.grash.dto.FileMiniDTO;
import com.grash.dto.FileShowDTO;
import com.grash.factory.StorageServiceFactory;
import com.grash.model.File;
import com.grash.service.StorageService;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Mapper(componentModel = "spring")
public abstract class FileMapper {

    @Lazy
    @Autowired
    private StorageServiceFactory storageServiceFactory;

    @Mappings({})
    public abstract FileMiniDTO toMiniDto(File model);

    public abstract FileShowDTO toShowDto(File model);

    @AfterMapping
    protected FileShowDTO toShowDto(File model, @MappingTarget FileShowDTO target) {
        target.setUrl(getSignedUrl(model));
        return target;
    }

    @AfterMapping
    protected FileMiniDTO toMiniDto(File model, @MappingTarget FileMiniDTO target) {
        target.setUrl(getSignedUrl(model));
        return target;
    }

    private String getSignedUrl(File file) {
        StorageService storageService = storageServiceFactory.getStorageService();
        return storageService.generateSignedUrl(file.getPath(), 60 * 3);
    }
}
