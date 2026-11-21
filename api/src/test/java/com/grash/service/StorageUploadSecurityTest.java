package com.grash.service;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.grash.storage.AttachmentLinks;
import com.grash.model.File;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageUploadSecurityTest {
    @Mock AttachmentLinks attachmentLinks;
    @Mock MinioClient minioClient;
    @Mock Storage storage;
    @InjectMocks MinioService minio;
    @InjectMocks GCPService gcp;

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(minio, "minioClient", minioClient);
        ReflectionTestUtils.setField(gcp, "storage", storage);
        ReflectionTestUtils.setField(minio, "configured", true);
        ReflectionTestUtils.setField(minio, "minioBucket", "test-bucket");
        ReflectionTestUtils.setField(gcp, "configured", true);
        ReflectionTestUtils.setField(gcp, "gcpBucketName", "test-bucket");
    }

    @Test
    void bothProvidersOnlyIssueProtectedApiLinks() {
        File file = new File();
        file.setPath("company/object");
        file.setName("report.pdf");
        when(attachmentLinks.sign("company/object", "report.pdf", 10)).thenReturn("https://cmms.test/api/files/content/token");
        assertThat(minio.generateSignedUrl(file, 10)).isEqualTo("https://cmms.test/api/files/content/token");
        assertThat(gcp.generateSignedUrl(file, 10)).isEqualTo("https://cmms.test/api/files/content/token");
        verify(attachmentLinks, times(2)).sign("company/object", "report.pdf", 10);
        verifyNoInteractions(minioClient, storage);
    }

    @Test
    void generatedAndReplacementUploadsAlsoForceDownload() throws Exception {
        byte[] bytes = "<svg/>".getBytes();
        minio.upload(bytes, "generated.pdf", "reports");
        minio.uploadAt(bytes, "old/logo.png", "image/png");
        gcp.upload(bytes, "generated.pdf", "reports");
        gcp.uploadAt(bytes, "old/logo.png", "image/png");
        ArgumentCaptor<PutObjectArgs> minioArgs = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient, times(2)).putObject(minioArgs.capture());
        for (PutObjectArgs args : minioArgs.getAllValues()) {
            assertThat(args.contentType()).isEqualTo("application/octet-stream");
            assertThat(args.headers().get("Content-Disposition")).isNotEmpty().allMatch(value -> value.startsWith("attachment;"));
        }
        ArgumentCaptor<BlobInfo> gcpArgs = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storage, times(2)).create(gcpArgs.capture(), any(byte[].class), any(Storage.BlobTargetOption.class));
        for (BlobInfo args : gcpArgs.getAllValues()) {
            assertThat(args.getContentType()).isEqualTo("application/octet-stream");
            assertThat(args.getContentDisposition()).startsWith("attachment;");
        }
    }

    @Test
    void minioDoesNotTrustTheClientMimeOrExtension() throws Exception {
        minio.upload(disguisedHtml(), "company 1");
        ArgumentCaptor<PutObjectArgs> args = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(args.capture());
        assertThat(args.getValue().contentType()).isEqualTo("application/octet-stream");
        assertThat(args.getValue().headers().get("Content-Disposition")).allMatch(v -> v.startsWith("attachment;"));
        assertThat(args.getValue().headers().get("Cache-Control")).contains("private, no-store");
    }

    @Test
    void gcpDoesNotTrustTheClientMimeOrExtension() throws Exception {
        gcp.upload(disguisedHtml(), "company 1");
        ArgumentCaptor<BlobInfo> args = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storage).createFrom(args.capture(), any(InputStream.class), any(Storage.BlobWriteOption.class));
        assertThat(args.getValue().getContentType()).isEqualTo("application/octet-stream");
        assertThat(args.getValue().getContentDisposition()).startsWith("attachment;");
        assertThat(args.getValue().getCacheControl()).isEqualTo("private, no-store");
    }

    private MockMultipartFile disguisedHtml() {
        return new MockMultipartFile("files", "report.pdf", "application/pdf",
                "<!doctype html><script>document.title='harmless-marker'</script>".getBytes());
    }
}
