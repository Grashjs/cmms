package com.grash.mapper;

import com.grash.dto.FileMiniDTO;
import com.grash.dto.FileShowDTO;
import com.grash.dto.FileThumbnailDTO;
import com.grash.factory.StorageServiceFactory;
import com.grash.model.File;
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
        return storageService.generateSignedUrl(file.getPath(), expirationInMinutes);
    }

    @Named("toThumbnailDto")
    public FileThumbnailDTO toThumbnailDto(File model) {
        if (model == null) return null;
        FileThumbnailDTO dto = new FileThumbnailDTO();
        dto.setId(model.getId());
        dto.setName(model.getName());
        dto.setUrl(getSignedUrl(model));
        dto.setThumbnailUrl(getThumbnailUrl(model));
        return dto;
    }

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
