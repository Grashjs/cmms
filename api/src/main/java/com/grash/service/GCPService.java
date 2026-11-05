package com.grash.service;

import com.google.auth.Credentials;
import com.grash.storage.AttachmentLinks;
import com.grash.storage.AttachmentPolicy;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.grash.exception.CustomException;
import com.grash.model.File;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class GCPService implements StorageService {
    @Value("${storage.gcp.value}")
    private String gcpJson;
    @Value("${storage.gcp.json-path}")
    private String gcpJsonPath;
    @Value("${storage.gcp.project-id}")
    private String gcpProjectId;
    @Value("${storage.gcp.bucket-name}")
    private String gcpBucketName;
    private Storage storage;
    private boolean configured = false;
    private final AttachmentLinks attachmentLinks;

    @PostConstruct
    private void init() {
        if (gcpJson.isEmpty() && gcpJsonPath.isEmpty()) {
            return;
        }
        Credentials credentials;
        try {
            InputStream is = gcpJson.isEmpty() ? Files.newInputStream(Paths.get(gcpJsonPath)) :
                    new ByteArrayInputStream(gcpJson.getBytes(StandardCharsets.UTF_8));
            credentials = GoogleCredentials.fromStream(is);
            configured = true;
        } catch (IOException e) {
            throw new CustomException("Wrong credentials", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        storage = StorageOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(gcpProjectId)
                .build()
                .getService();
    }

    public String upload(MultipartFile file, String folder) {
        checkIfConfigured();

        if (file == null || file.isEmpty()) {
            throw new CustomException("Uploaded file is empty.", HttpStatus.BAD_REQUEST);
        }

        String filePath = Helper.generateUniqueFilePath(file.getOriginalFilename(), folder);

        // Upload via InputStream (avoids loading the whole file into memory)
        try (InputStream inputStream = file.getInputStream()) {
            BlobId blobId = BlobId.of(gcpBucketName, filePath);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(AttachmentPolicy.DOWNLOAD_TYPE)
                    .setContentDisposition(AttachmentPolicy.disposition(file.getOriginalFilename(), false))
                    .setCacheControl(AttachmentPolicy.CACHE_CONTROL)
                    .build();

            storage.createFrom(blobInfo, inputStream,
                    Storage.BlobWriteOption.predefinedAcl(Storage.PredefinedAcl.PRIVATE));

            return filePath;
        } catch (IOException e) {
            log.error("Failed to read/write file during upload to {}", filePath, e);
            throw new CustomException("Failed to save the file.", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (StorageException e) {
            log.error("GCS error during upload to {}", filePath, e);
            throw new CustomException("Failed to save the file to storage.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String upload(byte[] data, String fileName, String folder) {
        checkIfConfigured();

        if (data == null || data.length == 0) {
            throw new CustomException("Uploaded file is empty.", HttpStatus.BAD_REQUEST);
        }

        String filePath = Helper.generateUniqueFilePath(fileName, folder);

        try {
            storage.create(
                    BlobInfo.newBuilder(BlobId.of(gcpBucketName, filePath))
                            .setContentType(AttachmentPolicy.DOWNLOAD_TYPE)
                            .setContentDisposition(AttachmentPolicy.disposition(fileName, false))
                            .setCacheControl(AttachmentPolicy.CACHE_CONTROL).build(),
                    data,
                    Storage.BlobTargetOption.predefinedAcl(Storage.PredefinedAcl.PRIVATE)
            );
            return filePath;
        } catch (StorageException e) {
            log.error("GCS error during upload to {}", filePath, e);
            throw new CustomException("Failed to save the file to storage.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void uploadAt(byte[] data, String filePath, String contentType) {
        checkIfConfigured();

        if (data == null || data.length == 0) {
            throw new CustomException("Uploaded file is empty.", HttpStatus.BAD_REQUEST);
        }

        try {
            storage.create(
                    BlobInfo.newBuilder(BlobId.of(gcpBucketName, filePath))
                            .setContentType(AttachmentPolicy.DOWNLOAD_TYPE)
                            .setContentDisposition(AttachmentPolicy.disposition(filePath, false))
                            .setCacheControl(AttachmentPolicy.CACHE_CONTROL)
                            .build(),
                    data,
                    Storage.BlobTargetOption.predefinedAcl(Storage.PredefinedAcl.PRIVATE)
            );
        } catch (StorageException e) {
            log.error("GCS error during upload to {}", filePath, e);
            throw new CustomException("Failed to save the file to storage.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public boolean exists(String filePath) {
        checkIfConfigured();
        Blob blob = storage.get(BlobId.of(gcpBucketName, filePath));
        return blob != null && blob.exists();
    }

    public byte[] download(String filePath) {
        checkIfConfigured();
        Blob blob = storage.get(BlobId.of(gcpBucketName, filePath));

        if (blob == null) {
            throw new CustomException("File not found", HttpStatus.NOT_FOUND);
        }
        try {
            return blob.getContent();
        } catch (StorageException e) {
            throw new CustomException("Error retrieving file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public byte[] download(File file) {
        checkIfConfigured();
        return download(file.getPath());
    }

    public String generateSignedUrl(File file, long expirationMinutes) {
        return attachmentLinks.sign(file.getPath(), file.getName(), expirationMinutes);
    }

    public String generateSignedUrl(String filePath, long expirationMinutes) {
        return attachmentLinks.sign(filePath, filePath, expirationMinutes);
    }

    public InputStream openStream(String filePath) {
        checkIfConfigured();
        Blob blob = storage.get(BlobId.of(gcpBucketName, filePath));
        if (blob == null) throw new CustomException("File not found", HttpStatus.NOT_FOUND);
        // Pin the generation: metadata/content must not change between lookup and reading.
        return Channels.newInputStream(blob.reader(Blob.BlobSourceOption.generationMatch()));
    }

    private void checkIfConfigured() {
        if (!configured)
            throw new CustomException("Google Cloud Storage is not configured. Please define the GCP credentials in " +
                    "the " +
                    "env variables", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
