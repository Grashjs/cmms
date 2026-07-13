package com.grash.service;

import com.grash.exception.CustomException;
import com.grash.model.File;
import com.grash.utils.Helper;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MinioService implements StorageService {
    @Value("${storage.minio.endpoint}")
    private String minioEndpoint;
    @Value("${storage.minio.bucket}")
    private String minioBucket;
    @Value("${storage.minio.access-key}")
    private String minioAccessKey;
    @Value("${storage.minio.secret-key}")
    private String minioSecretKey;
    @Value("${storage.minio.public-endpoint}")
    private String minioPublicEndpoint;

    private MinioClient minioClient;
    private static boolean configured = false;

    @PostConstruct
    private void init() {
        if (minioEndpoint.isEmpty() || minioBucket.isEmpty() || minioAccessKey.isEmpty() || minioSecretKey.isEmpty() || minioPublicEndpoint.isEmpty()) {
            return;
        }
        try {
            URI minioEndpointURI = new URI(minioEndpoint);
            MinioClient.Builder minioClientBuilder = MinioClient.builder()
                    .endpoint(minioPublicEndpoint)
                    .credentials(minioAccessKey, minioSecretKey);
            if (Helper.isLocalhost(minioPublicEndpoint)) minioClientBuilder.httpClient(
                    new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.HTTP,
                            new InetSocketAddress(minioEndpointURI.getHost(), minioEndpointURI.getPort()))).build()
            );
            minioClient = minioClientBuilder.build();
            // Check if the bucket exists, create if it doesn't
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioBucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioBucket).build());
            }
            configured = true;
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CustomException("Error configuring MinIO: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public String upload(MultipartFile file, String folder) {
        checkIfConfigured();
        Helper helper = new Helper();
        String filePath = folder + "/" + helper.generateString() + " " + file.getOriginalFilename();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioBucket)
                            .object(filePath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return filePath;
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CustomException(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public String generateSignedUrl(File file, long expirationMinutes) {
        return generateSignedUrl(file.getPath(), expirationMinutes);
    }

    public String generateSignedUrl(String filePath, long expirationMinutes) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioBucket)
                            .object(filePath)
                            .expiry(Math.toIntExact(expirationMinutes), TimeUnit.MINUTES)
                            .build()
            );
            return url;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public byte[] download(String filePath) {
        checkIfConfigured();
        InputStream inputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioBucket)
                            .object(filePath)
                            .build()
            );
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CustomException("Error retrieving file", HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                byteArrayOutputStream.close();
            } catch (IOException e) {
                throw new CustomException("Error closing stream", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    public byte[] download(File file) {
        checkIfConfigured();
        URI uri;
        try {
            uri = new URI(file.getPath());
        } catch (URISyntaxException e) {
            throw new CustomException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        String path = uri.getPath();
        String filePath = "company " + file.getCompany().getId() + "/" + path.substring(path.lastIndexOf('/') + 1);
        return download(filePath);
    }

    private void checkIfConfigured() {
        if (!configured)
            throw new CustomException("MinIO is not configured. Please define the MinIO credentials in the env " +
                    "variables",
                    HttpStatus.INTERNAL_SERVER_ERROR);
    }
}