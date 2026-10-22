package com.grash.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import io.minio.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

/** Standalone maintenance command: no Spring context, jobs, mail or database writes. Dry-run by default. */
public final class StorageMetadataMigration {
    public record Result(long scanned, long changed, boolean applied) { }

    public static Result minio(MinioClient client, String bucket, String prefix, boolean apply) throws Exception {
        long scanned = 0, changed = 0;
        for (var result : client.listObjects(ListObjectsArgs.builder().bucket(bucket).prefix(prefix).recursive(true).build())) {
            var item = result.get();
            if (item.isDir()) continue;
            var stat = client.statObject(StatObjectArgs.builder().bucket(bucket).object(item.objectName()).build());
            scanned++;
            var headers = AttachmentPolicy.storageHeaders(item.objectName());
            boolean safe = AttachmentPolicy.safeStorageMetadata(stat.contentType(), stat.headers().get("Content-Disposition"),
                    stat.headers().get("Cache-Control"), stat.headers().get("Content-Encoding"));
            if (safe) continue;
            changed++;
            if (apply) {
                // Freeze uploads first: S3 CopyObject has no portable destination metageneration precondition.
                // Source ETag matching rejects content that changed since inspection. Preserve custom metadata/tags.
                client.copyObject(CopyObjectArgs.builder().bucket(bucket).object(item.objectName())
                        .source(CopySource.builder().bucket(bucket).object(item.objectName()).matchETag(stat.etag()).build())
                        .metadataDirective(Directive.REPLACE).taggingDirective(Directive.COPY)
                        .headers(headers).userMetadata(stat.userMetadata()).build());
            }
        }
        return new Result(scanned, changed, apply);
    }

    public static Result gcp(Storage storage, String bucket, String prefix, boolean apply) {
        long scanned = 0, changed = 0;
        // GCS permits metadata updates on individual generations; include noncurrent generations too.
        for (Blob blob : storage.list(bucket, Storage.BlobListOption.prefix(prefix),
                Storage.BlobListOption.versions(true)).iterateAll()) {
            scanned++;
            String disposition = AttachmentPolicy.disposition(blob.getName(), false);
            if (AttachmentPolicy.safeStorageMetadata(blob.getContentType(), blob.getContentDisposition(),
                    blob.getCacheControl(), blob.getContentEncoding())) continue;
            changed++;
            if (apply) {
                storage.update(blob.toBuilder().setContentType(AttachmentPolicy.DOWNLOAD_TYPE)
                                .setContentDisposition(disposition).setCacheControl(AttachmentPolicy.CACHE_CONTROL)
                                .setContentEncoding(null).build(),
                        Storage.BlobTargetOption.generationMatch(), Storage.BlobTargetOption.metagenerationMatch());
            }
        }
        return new Result(scanned, changed, apply);
    }

    public static void main(String[] args) throws Exception {
        Set<String> options = Set.copyOf(Arrays.asList(args));
        if (options.stream().anyMatch(a -> !Set.of("--apply", "--uploads-paused").contains(a))) {
            throw new IllegalArgumentException("Usage: StorageMetadataMigration [--apply --uploads-paused]; configure STORAGE_TYPE and storage environment variables");
        }
        boolean apply = options.contains("--apply");
        if (apply && !options.contains("--uploads-paused")) {
            throw new IllegalArgumentException("Pause uploads before applying; acknowledge with --uploads-paused");
        }
        String prefix = System.getenv().getOrDefault("STORAGE_MIGRATION_PREFIX", "");
        Result result;
        if ("MINIO".equalsIgnoreCase(required("STORAGE_TYPE"))) {
            try (MinioClient client = MinioClient.builder().endpoint(required("MINIO_ENDPOINT"))
                    .credentials(required("MINIO_ACCESS_KEY"), required("MINIO_SECRET_KEY")).build()) {
                result = minio(client, required("MINIO_BUCKET"), prefix, apply);
            }
            System.out.println("S3 noncurrent versions retain their old metadata. Revoke old signing credentials and block direct/versioned access before reopening.");
        } else if ("GCP".equalsIgnoreCase(required("STORAGE_TYPE"))) {
            GoogleCredentials credentials;
            String json = System.getenv().getOrDefault("GCP_JSON", "");
            String jsonPath = System.getenv().getOrDefault("GCP_JSON_PATH", "");
            if (!json.isBlank()) {
                credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
            } else if (!jsonPath.isBlank()) {
                try (var input = Files.newInputStream(Path.of(jsonPath))) { credentials = GoogleCredentials.fromStream(input); }
            } else {
                credentials = GoogleCredentials.getApplicationDefault();
            }
            try (Storage storage = StorageOptions.newBuilder().setCredentials(credentials)
                    .setProjectId(required("GCP_PROJECT_ID")).build().getService()) {
                result = gcp(storage, required("GCP_BUCKET_NAME"), prefix, apply);
            }
        } else throw new IllegalArgumentException("STORAGE_TYPE must be MINIO or GCP");
        System.out.printf("mode=%s scanned=%d %s=%d%n", apply ? "apply" : "dry-run", result.scanned(),
                apply ? "updated" : "would-update", result.changed());
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing environment variable: " + name);
        return value;
    }
}
