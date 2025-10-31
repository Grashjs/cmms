package com.grash.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.grash.exception.CustomException;
import com.grash.model.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GCPServiceTest {

    @InjectMocks
    private GCPService gcpService;

    @Mock
    private Storage storage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gcpService, "gcpProjectId", "test-project");
        ReflectionTestUtils.setField(gcpService, "gcpBucketName", "test-bucket");
    }

    @Nested
    @DisplayName("Configuration and Initialization")
    class InitTests {

        @Test
        @DisplayName("should initialize with JSON string")
        void init_withJson() throws Exception {
            try (MockedStatic<StorageOptions> storageOptionsMockedStatic = mockStatic(StorageOptions.class);
                 MockedStatic<GoogleCredentials> credentialsMockedStatic = mockStatic(GoogleCredentials.class)) {
                StorageOptions.Builder builder = mock(StorageOptions.Builder.class);
                StorageOptions storageOptions = mock(StorageOptions.class);
                storageOptionsMockedStatic.when(StorageOptions::newBuilder).thenReturn(builder);
                when(builder.setCredentials(any())).thenReturn(builder);
                when(builder.setProjectId(any())).thenReturn(builder);
                when(builder.build()).thenReturn(storageOptions);
                when(storageOptions.getService()).thenReturn(storage);

                ReflectionTestUtils.setField(gcpService, "gcpJson", "{}");
                ReflectionTestUtils.setField(gcpService, "gcpJsonPath", "");
                credentialsMockedStatic.when(() -> GoogleCredentials.fromStream(any())).thenReturn(null);

                invokeInit();

                assertTrue(isConfigured());
            }
        }

        @Test
        @DisplayName("should initialize with JSON path")
        void init_withJsonPath() throws Exception {
            try (MockedStatic<StorageOptions> storageOptionsMockedStatic = mockStatic(StorageOptions.class);
                 MockedStatic<GoogleCredentials> credentialsMockedStatic = mockStatic(GoogleCredentials.class);
                 MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
                StorageOptions.Builder builder = mock(StorageOptions.Builder.class);
                StorageOptions storageOptions = mock(StorageOptions.class);
                storageOptionsMockedStatic.when(StorageOptions::newBuilder).thenReturn(builder);
                when(builder.setCredentials(any())).thenReturn(builder);
                when(builder.setProjectId(any())).thenReturn(builder);
                when(builder.build()).thenReturn(storageOptions);
                when(storageOptions.getService()).thenReturn(storage);

                ReflectionTestUtils.setField(gcpService, "gcpJson", "");
                ReflectionTestUtils.setField(gcpService, "gcpJsonPath", "/path");
                filesMockedStatic.when(() -> Files.newInputStream(any())).thenReturn(null);
                credentialsMockedStatic.when(() -> GoogleCredentials.fromStream(any())).thenReturn(null);

                invokeInit();

                assertTrue(isConfigured());
            }
        }

        @Test
        @DisplayName("should not initialize if no json is provided")
        void init_noJson() throws Exception {
            ReflectionTestUtils.setField(gcpService, "gcpJson", "");
            ReflectionTestUtils.setField(gcpService, "gcpJsonPath", "");

            invokeInit();

            assertFalse(isConfigured());
        }

        @Test
        @DisplayName("should throw exception on IOException")
        void init_ioException() {
            try (MockedStatic<GoogleCredentials> credentialsMockedStatic = mockStatic(GoogleCredentials.class)) {
                ReflectionTestUtils.setField(gcpService, "gcpJson", "{}");
                ReflectionTestUtils.setField(gcpService, "gcpJsonPath", "");
                credentialsMockedStatic.when(() -> GoogleCredentials.fromStream(any())).thenThrow(IOException.class);

                assertThrows(Exception.class, this::invokeInit);
            }
        }

        @Test
        @DisplayName("should throw exception if not configured")
        void notConfigured() {
            setConfigured(false);
            CustomException e = assertThrows(CustomException.class, () -> gcpService.download("path"));
            assertTrue(e.getMessage().contains("Google Cloud Storage is not configured"));
        }

        private void invokeInit() throws Exception {
            Method init = GCPService.class.getDeclaredMethod("init");
            init.setAccessible(true);
            init.invoke(gcpService);
        }
    }

    @Nested
    @DisplayName("File Operations")
    class FileOpTests {

        @BeforeEach
        void configure() {
            ReflectionTestUtils.setField(gcpService, "storage", storage);
            setConfigured(true);
        }

        @Test
        @DisplayName("should upload a file")
        void upload() throws IOException {
            MockMultipartFile multipartFile = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
            when(storage.create(any(BlobInfo.class), any(byte[].class), any(Storage.BlobTargetOption.class))).thenReturn(null);

            String filePath = gcpService.upload(multipartFile, "folder");

            assertNotNull(filePath);
            assertTrue(filePath.startsWith("folder/"));
            assertTrue(filePath.endsWith(" test.txt"));
        }

        @Test
        @DisplayName("should download a file")
        void download() {
            Blob blob = mock(Blob.class);
            when(blob.getContent()).thenReturn("content".getBytes());
            when(storage.get(any(BlobId.class))).thenReturn(blob);

            byte[] content = gcpService.download("path");

            assertArrayEquals("content".getBytes(), content);
        }

        @Test
        @DisplayName("should throw not found when downloading non-existent file")
        void downloadNotFound() {
            when(storage.get(any(BlobId.class))).thenReturn(null);
            assertThrows(CustomException.class, () -> gcpService.download("path"));
        }

        @Test
        @DisplayName("should download a file by File object")
        void downloadByFile() {
            File file = new File();
            file.setPath("path");
            Blob blob = mock(Blob.class);
            when(blob.getContent()).thenReturn("content".getBytes());
            when(storage.get(any(BlobId.class))).thenReturn(blob);

            byte[] content = gcpService.download(file);

            assertArrayEquals("content".getBytes(), content);
        }

        @Test
        @DisplayName("should generate a signed URL")
        void generateSignedUrl() throws Exception {
            Blob blob = mock(Blob.class);
            BlobId blobId = BlobId.of("bucket", "name");
            when(blob.getBlobId()).thenReturn(blobId);
            when(storage.get(any(BlobId.class))).thenReturn(blob);
            when(storage.signUrl(any(BlobInfo.class), anyLong(), any(), any(Storage.SignUrlOption.class)))
                    .thenReturn(new URL("http://signed-url.com"));

            String url = gcpService.generateSignedUrl("path", 10);

            assertEquals("http://signed-url.com", url);
        }

        @Test
        @DisplayName("should generate a signed URL by File object")
        void generateSignedUrlByFile() throws Exception {
            File file = new File();
            file.setPath("path");
            Blob blob = mock(Blob.class);
            BlobId blobId = BlobId.of("bucket", "name");
            when(blob.getBlobId()).thenReturn(blobId);
            when(storage.get(any(BlobId.class))).thenReturn(blob);
            when(storage.signUrl(any(BlobInfo.class), anyLong(), any(), any(Storage.SignUrlOption.class)))
                    .thenReturn(new URL("http://signed-url.com"));

            String url = gcpService.generateSignedUrl(file, 10);

            assertEquals("http://signed-url.com", url);
        }

        @Test
        @DisplayName("should throw exception on upload IO error")
        void upload_IOException() throws IOException {
            MockMultipartFile multipartFile = mock(MockMultipartFile.class);
            when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
            when(multipartFile.getBytes()).thenThrow(IOException.class);

            assertThrows(CustomException.class, () -> gcpService.upload(multipartFile, "folder"));
        }

        @Test
        @DisplayName("should throw exception on download storage error")
        void download_storageException() {
            Blob blob = mock(Blob.class);
            when(blob.getContent()).thenThrow(StorageException.class);
            when(storage.get(any(BlobId.class))).thenReturn(blob);

            assertThrows(CustomException.class, () -> gcpService.download("path"));
        }

        @Test
        @DisplayName("should throw exception on signed URL storage error")
        void generateSignedUrl_storageException() {
            Blob blob = mock(Blob.class);
            BlobId blobId = BlobId.of("bucket", "name");
            when(blob.getBlobId()).thenReturn(blobId);
            when(storage.get(any(BlobId.class))).thenReturn(blob);
            when(storage.signUrl(any(BlobInfo.class), anyLong(), any(), any(Storage.SignUrlOption.class)))
                    .thenThrow(StorageException.class);

            assertThrows(CustomException.class, () -> gcpService.generateSignedUrl("path", 10));
        }

        @Test
        @DisplayName("should throw not found when generating signed URL for non-existent file")
        void generateSignedUrl_notFound() {
            when(storage.get(any(BlobId.class))).thenReturn(null);
            assertThrows(CustomException.class, () -> gcpService.generateSignedUrl("path", 10));
        }
    }

    private void setConfigured(boolean value) {
        try {
            java.lang.reflect.Field configuredField = GCPService.class.getDeclaredField("configured");
            configuredField.setAccessible(true);
            configuredField.set(null, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isConfigured() {
        try {
            java.lang.reflect.Field configuredField = GCPService.class.getDeclaredField("configured");
            configuredField.setAccessible(true);
            return (boolean) configuredField.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}