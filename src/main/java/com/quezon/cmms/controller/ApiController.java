package com.quezon.cmms.controller;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.sql.DataSource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@RestController
public class ApiController {

    private final JdbcTemplate jdbcTemplate;
    private final S3Client s3;
    private final String bucket;

    public ApiController(DataSource ds,
                         S3Client s3Client,
                         @Value("${S3_BUCKET:}") String bucket) {
        this.jdbcTemplate = new JdbcTemplate(ds);
        this.s3 = s3Client;
        this.bucket = bucket;
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body("{\"status\":\"ok\"}");
    }

    @GetMapping("/db-test")
    public ResponseEntity<?> dbTest() {
        try {
            Integer v = jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Integer.class);
            return ResponseEntity.ok().body("{\"ok\": true, \"val\": " + v + "}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file) {
        if (bucket == null || bucket.isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\":\"S3_BUCKET not configured\"}");
        }
        String key = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3.putObject(req, software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));
            return ResponseEntity.ok().body("{\"uploaded\": true, \"key\":\"" + key + "\"}");
        } catch (IOException | S3Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/download/{key}")
    public void download(@PathVariable String key, HttpServletResponse response) throws IOException {
        if (bucket == null || bucket.isBlank()) {
            response.setStatus(400);
            response.getWriter().write("{\"error\":\"S3_BUCKET not configured\"}");
            return;
        }
        try {
            GetObjectRequest req = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3.getObject(req);
            String contentType = s3Object.response().contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + "\"");
            response.setContentType(contentType);
            response.setContentLengthLong(s3Object.response().contentLength() != null ? s3Object.response().contentLength() : -1);

            IOUtils.copy(s3Object, response.getOutputStream());
            response.flushBuffer();
            s3Object.close();
        } catch (NoSuchKeyException e) {
            response.setStatus(404);
            response.getWriter().write("{\"error\":\"object not found\"}");
        } catch (S3Exception e) {
            response.setStatus(500);
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
