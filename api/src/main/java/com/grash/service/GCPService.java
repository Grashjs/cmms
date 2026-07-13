package com.grash.service;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.grash.exception.CustomException;
import com.grash.model.File;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
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
    private static boolean configured = false;

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
        Helper helper = new Helper();
        try {
            String filePath = folder + "/" + helper.generateString() + " " + file.getOriginalFilename();
            BlobInfo blobInfo = storage.create(
                    BlobInfo.newBuilder(gcpBucketName, filePath
                    ).build(), //get
                    // original file name
                    file.getBytes(),
                    Storage.BlobTargetOption.predefinedAcl(Storage.PredefinedAcl.PRIVATE)
            );
            return filePath;
        } catch (IllegalStateException | IOException e) {
            throw new CustomException(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
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

    private Blob getBlob(String filePath) {
        Blob blob = storage.get(BlobId.of(gcpBucketName, filePath));

        if (blob == null) {
            throw new CustomException("File not found", HttpStatus.NOT_FOUND);
        }
        return blob;
    }

    public String generateSignedUrl(File file, long expirationMinutes) {
        return generateSignedUrl(file.getPath(), expirationMinutes);
    }

    public String generateSignedUrl(String filePath, long expirationMinutes) {
        Blob blob = getBlob(filePath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blob.getBlobId()).setContentType(blob.getContentType()).build();
        return generateSignedUrl(blobInfo, expirationMinutes);
    }

    public String generateSignedUrl(BlobInfo blobInfo, long expirationMinutes) {
        try {
            return storage.signUrl(blobInfo, expirationMinutes, java.util.concurrent.TimeUnit.MINUTES,
                            Storage.SignUrlOption.withV4Signature())
                    .toString();
        } catch (StorageException e) {
            throw new CustomException("Error generating signed URL: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void checkIfConfigured() {
        if (!configured)
            throw new CustomException("Google Cloud Storage is not configured. Please define the GCP credentials in " +
                    "the " +
                    "env variables", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
