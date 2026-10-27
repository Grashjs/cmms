package com.grash.service;

import com.grash.exception.CustomException;
import com.grash.model.File;
import com.grash.utils.Helper;
import io.minio.*;
import io.minio.errors.*;
import com.grash.storage.AttachmentLinks;
import com.grash.storage.AttachmentPolicy;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService implements StorageService {
    @Value("${storage.minio.endpoint}")
    private String minioEndpoint;
    @Value("${storage.minio.bucket}")
    private String minioBucket;
    @Value("${storage.minio.access-key}")
    private String minioAccessKey;
    @Value("${storage.minio.secret-key}")
    private String minioSecretKey;
    private final AttachmentLinks attachmentLinks;

    private MinioClient minioClient;
    private boolean configured = false;

    @PostConstruct
    private void init() {
        if (minioEndpoint.isEmpty() || minioBucket.isEmpty() || minioAccessKey.isEmpty() || minioSecretKey.isEmpty()) {
            return;
        }
        try {
            minioClient = MinioClient.builder().endpoint(minioEndpoint)
                    .credentials(minioAccessKey, minioSecretKey).build();
            // Check if the bucket exists, create if it doesn't
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioBucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioBucket).build());
            }
            configured = true;
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CustomException("Error configuring MinIO: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String upload(MultipartFile file, String folder) {
        checkIfConfigured();

        if (file == null || file.isEmpty()) {
            throw new CustomException("Uploaded file is empty.", HttpStatus.BAD_REQUEST);
        }

        String filePath = Helper.generateUniqueFilePath(file.getOriginalFilename(), folder);
        try (InputStream input = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioBucket)
                            .object(filePath)
                            .stream(input, file.getSize(), -1)
                            .contentType(AttachmentPolicy.DOWNLOAD_TYPE)
                            .headers(AttachmentPolicy.storageHeaders(file.getOriginalFilename()))
                            .build()
            );

            return filePath;
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("MinIO error during upload to {}", filePath, e);
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
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioBucket)
                            .object(filePath)
                            .stream(inputStream, data.length, -1)
                            .contentType(AttachmentPolicy.DOWNLOAD_TYPE)
                            .headers(AttachmentPolicy.storageHeaders(fileName))
                            .build()
            );
            return filePath;
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("MinIO error during upload to {}", filePath, e);
            throw new CustomException("Failed to save the file to storage.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void uploadAt(byte[] data, String filePath, String contentType) {
        checkIfConfigured();

        if (data == null || data.length == 0) {
            throw new CustomException("Uploaded file is empty.", HttpStatus.BAD_REQUEST);
        }

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioBucket)
                            .object(filePath)
                            .stream(inputStream, data.length, -1)
                            .contentType(AttachmentPolicy.DOWNLOAD_TYPE)
                            .headers(AttachmentPolicy.storageHeaders(filePath))
                            .build()
            );
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("MinIO error during upload to {}", filePath, e);
            throw new CustomException("Failed to save the file to storage.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public boolean exists(String filePath) {
        checkIfConfigured();
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioBucket)
                            .object(filePath)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String generateSignedUrl(File file, long expirationMinutes) {
        return attachmentLinks.sign(file.getPath(), file.getName(), expirationMinutes);
    }

    public String generateSignedUrl(String filePath, long expirationMinutes) {
        return attachmentLinks.sign(filePath, filePath, expirationMinutes);
    }

    public InputStream openStream(String filePath) {
        checkIfConfigured();
        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(minioBucket).object(filePath).build());
        } catch (ErrorResponseException e) {
            throw new CustomException("File not found", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            throw new CustomException("Error retrieving file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public byte[] download(String filePath) {
        try (InputStream input = openStream(filePath)) {
            return input.readAllBytes();
        } catch (IOException e) {
            throw new CustomException("Error retrieving file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public byte[] download(File file) {
        checkIfConfigured();
        return download(file.getPath());
    }

    private void checkIfConfigured() {
        if (!configured)
            throw new CustomException("MinIO is not configured. Please define the MinIO credentials in the env " +
                    "variables",
                    HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
