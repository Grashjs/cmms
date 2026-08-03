package com.grash.mapper;

import com.grash.dto.AssetMiniDTO;
import com.grash.dto.FileMiniDTO;
import com.grash.dto.FileShowDTO;
import com.grash.dto.FileThumbnailDTO;
import com.grash.dto.WorkOrderBaseMiniDTO;
import com.grash.factory.StorageServiceFactory;
import com.grash.model.Asset;
import com.grash.model.File;
import com.grash.model.WorkOrder;
import com.grash.model.enums.FileType;
import com.grash.repository.FileRepository;
import com.grash.service.StorageService;
import com.grash.utils.Helper;
import net.coobird.thumbnailator.Thumbnails;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class FileMapper {

    @Lazy
    @Autowired
    private StorageServiceFactory storageServiceFactory;
    @Lazy
    @Autowired
    private FileRepository fileRepository;

    private final Integer expirationInMinutes = 60 * 3;


    @Mappings({})
    public abstract FileMiniDTO toMiniDto(File model);

    // The linked assets and work orders are filled by hand below rather than through
    // uses = {AssetMapper.class, WorkOrderMapper.class}: both of those mappers already use
    // FileMapper, and the resulting bean cycle fails at startup because Spring Boot rejects
    // circular references by default.
    @Mapping(target = "assets", ignore = true)
    @Mapping(target = "workOrders", ignore = true)
    public abstract FileShowDTO toShowDto(File model);

    @AfterMapping
    protected FileShowDTO toShowDto(File model, @MappingTarget FileShowDTO target) {
        target.setUrl(getSignedUrl(model));
        target.setAssets(model.getAssets().stream().map(this::toAssetMiniDto).collect(Collectors.toList()));
        target.setWorkOrders(model.getWorkOrders().stream().map(this::toWorkOrderMiniDto).collect(Collectors.toList()));
        return target;
    }

    private AssetMiniDTO toAssetMiniDto(Asset asset) {
        AssetMiniDTO dto = new AssetMiniDTO();
        dto.setId(asset.getId());
        dto.setName(asset.getName());
        dto.setCustomId(asset.getCustomId());
        dto.setParentId(asset.getParentAsset() == null ? null : asset.getParentAsset().getId());
        dto.setLocationId(asset.getLocation() == null ? null : asset.getLocation().getId());
        return dto;
    }

    private WorkOrderBaseMiniDTO toWorkOrderMiniDto(WorkOrder workOrder) {
        WorkOrderBaseMiniDTO dto = new WorkOrderBaseMiniDTO();
        dto.setId(workOrder.getId());
        dto.setTitle(workOrder.getTitle());
        dto.setCustomId(workOrder.getCustomId());
        dto.setStatus(workOrder.getStatus());
        dto.setPriority(workOrder.getPriority());
        dto.setDueDate(workOrder.getDueDate());
        dto.setCreatedAt(workOrder.getCreatedAt());
        return dto;
    }

    @AfterMapping
    protected FileMiniDTO toMiniDto(File model, @MappingTarget FileMiniDTO target) {
        target.setUrl(getSignedUrl(model));
        return target;
    }

    private String getSignedUrl(File file) {
        StorageService storageService = storageServiceFactory.getStorageService();
        return storageService.generateSignedUrl(file.getPath(), expirationInMinutes);
    }

    @Named("toThumbnailDto")
    public FileThumbnailDTO toThumbnailDto(File model) {
        if (model == null) return null;
        FileThumbnailDTO dto = new FileThumbnailDTO();
        dto.setId(model.getId());
        dto.setName(model.getName());
        dto.setUrl(getSignedUrl(model));
        dto.setThumbnailUrl(dto.getUrl());
        return dto;
    }

    //TODO generate thumbnail on upload and run once a migration for existing images instead of generating on the fly
    private String getThumbnailUrl(File file) {
        StorageService storageService = storageServiceFactory.getStorageService();

        if (file.getThumbnailPath() != null) {
            return storageService.generateSignedUrl(file.getThumbnailPath(), expirationInMinutes);
        }

        if (file.getType() != FileType.IMAGE) {
            return null;
        }

        try {
            byte[] originalBytes = storageService.download(file);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Thumbnails.of(inputStream)
                    .size(200, 200)
                    .outputFormat("jpg")
                    .toOutputStream(outputStream);

            byte[] thumbnailBytes = outputStream.toByteArray();

            Helper helper = new Helper();
            String thumbFileName = helper.generateString() + "_thumb.jpg";
            String folder = "";
            if (file.getPath().contains("/")) {
                folder = file.getPath().substring(0, file.getPath().lastIndexOf('/'));
            }

            String thumbnailPath = storageService.upload(thumbnailBytes, thumbFileName, folder);

            file.setThumbnailPath(thumbnailPath);
            fileRepository.save(file);

            return storageService.generateSignedUrl(thumbnailPath, expirationInMinutes);
        } catch (IOException e) {
            return null;
        }
    }
}
