package com.grash.service;

import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.File;
import io.minio.*;
import io.minio.errors.MinioException;
import okhttp3.Headers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {

    @InjectMocks
    private MinioService minioService;

    @Mock
    private MinioClient minioClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(minioService, "minioEndpoint", "http://localhost:9000");
        ReflectionTestUtils.setField(minioService, "minioBucket", "test-bucket");
        ReflectionTestUtils.setField(minioService, "minioAccessKey", "test-access-key");
        ReflectionTestUtils.setField(minioService, "minioSecretKey", "test-secret-key");
        ReflectionTestUtils.setField(minioService, "minioPublicEndpoint", "http://localhost:9000");
        ReflectionTestUtils.setField(minioService, "minioClient", minioClient);
    }

    @Nested
    @DisplayName("Upload Tests")
    class UploadTests {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(minioService, "configured", true);
        }

        @Test
        @DisplayName("Should upload file successfully")
        void shouldUploadFileSuccessfully() throws Exception {
            MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test data".getBytes());
            String folder = "test-folder";

            when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

            String filePath = minioService.upload(file, folder);

            assertNotNull(filePath);
            assertTrue(filePath.startsWith(folder + "/"));
            assertTrue(filePath.endsWith(" test.txt"));

            verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("Should throw CustomException when MinIO is not configured")
        void shouldThrowCustomExceptionWhenNotConfigured() {
            ReflectionTestUtils.setField(minioService, "configured", false);
            MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test data".getBytes());

            CustomException exception = assertThrows(CustomException.class, () -> minioService.upload(file, "folder"));
            assertEquals("MinIO is not configured. Please define the MinIO credentials in the env variables", exception.getMessage());
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw CustomException on MinioException")
        void shouldThrowCustomExceptionOnMinioException() throws Exception {
            MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test data".getBytes());
            String folder = "test-folder";

            when(minioClient.putObject(any(PutObjectArgs.class))).thenAnswer(invocation -> {
                throw new MinioException("MinIO error");
            });

            CustomException exception = assertThrows(CustomException.class, () -> minioService.upload(file, folder));
            assertEquals("MinIO error", exception.getMessage());
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("Generate Signed URL Tests")
    class GenerateSignedUrlTests {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(minioService, "configured", true);
        }

        @Test
        @DisplayName("Should generate signed URL from File object")
        void shouldGenerateSignedUrlFromFileObject() throws Exception {
            File file = new File();
            file.setPath("test-folder/test.txt");
            long expirationMinutes = 60;
            String expectedUrl = "http://localhost:9000/test-bucket/test-folder/test.txt?signed";

            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(expectedUrl);

            String signedUrl = minioService.generateSignedUrl(file, expirationMinutes);

            assertEquals(expectedUrl, signedUrl);
            verify(minioClient, times(1)).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
        }

        @Test
        @DisplayName("Should generate signed URL from file path")
        void shouldGenerateSignedUrlFromFilePath() throws Exception {
            String filePath = "test-folder/test.txt";
            long expirationMinutes = 60;
            String expectedUrl = "http://localhost:9000/test-bucket/test-folder/test.txt?signed";

            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(expectedUrl);

            String signedUrl = minioService.generateSignedUrl(filePath, expirationMinutes);

            assertEquals(expectedUrl, signedUrl);
            verify(minioClient, times(1)).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
        }

        @Test
        @DisplayName("Should throw RuntimeException on error")
        void shouldThrowRuntimeExceptionOnError() throws Exception {
            String filePath = "test-folder/test.txt";
            long expirationMinutes = 60;

            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenThrow(new RuntimeException("URL generation error"));

            assertThrows(RuntimeException.class, () -> minioService.generateSignedUrl(filePath, expirationMinutes));
        }
    }

    @Nested
    @DisplayName("Download Tests")
    class DownloadTests {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(minioService, "configured", true);
        }

        @Test
        @DisplayName("Should download file successfully from file path")
        void shouldDownloadFileSuccessfullyFromFilePath() throws Exception {
            String filePath = "test-folder/test.txt";
            byte[] expectedData = "test data".getBytes();
            InputStream inputStream = new ByteArrayInputStream(expectedData);
            GetObjectResponse getObjectResponse = new GetObjectResponse(new Headers.Builder().build(), "test-bucket", "", "test.txt", inputStream);

            when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);

            byte[] downloadedData = minioService.download(filePath);

            assertArrayEquals(expectedData, downloadedData);
            verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
        }

        @Test
        @DisplayName("Should download file successfully from File object")
        void shouldDownloadFileSuccessfullyFromFileObject() throws Exception {
            Company company = new Company();
            company.setId(1L);
            File file = new File();
            file.setPath("http://localhost:9000/test-bucket/test.txt");
            file.setCompany(company);

            byte[] expectedData = "test data".getBytes();
            InputStream inputStream = new ByteArrayInputStream(expectedData);
            GetObjectResponse getObjectResponse = new GetObjectResponse(new Headers.Builder().build(), "test-bucket", "", "test.txt", inputStream);

            when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);

            byte[] downloadedData = minioService.download(file);

            assertArrayEquals(expectedData, downloadedData);
            verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
        }

        @Test
        @DisplayName("Should throw CustomException when MinIO is not configured")
        void shouldThrowCustomExceptionWhenNotConfigured() {
            ReflectionTestUtils.setField(minioService, "configured", false);

            CustomException exception = assertThrows(CustomException.class, () -> minioService.download("filePath"));
            assertEquals("MinIO is not configured. Please define the MinIO credentials in the env variables", exception.getMessage());
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw CustomException on MinioException")
        void shouldThrowCustomExceptionOnMinioException() throws Exception {
            String filePath = "test-folder/test.txt";

            when(minioClient.getObject(any(GetObjectArgs.class))).thenAnswer(invocation -> {
                throw new MinioException("MinIO error");
            });

            CustomException exception = assertThrows(CustomException.class, () -> minioService.download(filePath));
            assertEquals("Error retrieving file", exception.getMessage());
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("Init Method Tests")
    class InitMethodTests {

        private MockedStatic<MinioClient> minioClientStaticMock;

        @BeforeEach
        void setUp() {
            minioClientStaticMock = Mockito.mockStatic(MinioClient.class);
            ReflectionTestUtils.setField(MinioService.class, "configured", false);
        }

        @AfterEach
        void tearDown() {
            minioClientStaticMock.close();
        }

        @Test
        @DisplayName("Should not configure if properties are empty")
        void shouldNotConfigureIfPropertiesAreEmpty() throws Exception {
            ReflectionTestUtils.setField(minioService, "minioEndpoint", "");
            invokeInit();
            assertFalse((Boolean) ReflectionTestUtils.getField(MinioService.class, "configured"));
        }

        @Test
        @DisplayName("Should configure and create bucket if it does not exist")
        void shouldConfigureAndCreateBucket() throws Exception {
            MinioClient.Builder builder = mock(MinioClient.Builder.class);
            when(builder.endpoint(anyString())).thenReturn(builder);
            when(builder.credentials(anyString(), anyString())).thenReturn(builder);
            when(builder.build()).thenReturn(minioClient);
            minioClientStaticMock.when(MinioClient::builder).thenReturn(builder);

            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

            invokeInit();

            assertTrue((Boolean) ReflectionTestUtils.getField(MinioService.class, "configured"));
            verify(minioClient, times(1)).makeBucket(any());
        }

        @Test
        @DisplayName("Should configure and not create bucket if it exists")
        void shouldConfigureAndNotCreateBucket() throws Exception {
            MinioClient.Builder builder = mock(MinioClient.Builder.class);
            when(builder.endpoint(anyString())).thenReturn(builder);
            when(builder.credentials(anyString(), anyString())).thenReturn(builder);
            when(builder.build()).thenReturn(minioClient);
            minioClientStaticMock.when(MinioClient::builder).thenReturn(builder);

            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            invokeInit();

            assertTrue((Boolean) ReflectionTestUtils.getField(MinioService.class, "configured"));
            verify(minioClient, never()).makeBucket(any());
        }

        @Test
        @DisplayName("Should throw CustomException on MinioException during init")
        void shouldThrowCustomExceptionOnInit() throws Exception {
            MinioClient.Builder builder = mock(MinioClient.Builder.class);
            when(builder.endpoint(anyString())).thenReturn(builder);
            when(builder.credentials(anyString(), anyString())).thenReturn(builder);
            when(builder.build()).thenReturn(minioClient);
            minioClientStaticMock.when(MinioClient::builder).thenReturn(builder);

            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenAnswer(invocation -> {
                throw new MinioException("Init error");
            });

            InvocationTargetException invocationTargetException = assertThrows(InvocationTargetException.class, this::invokeInit);
            assertTrue(invocationTargetException.getTargetException() instanceof CustomException);
            assertTrue(invocationTargetException.getTargetException().getMessage().contains("Error configuring MinIO"));
        }

        private void invokeInit() throws Exception {
            Method init = MinioService.class.getDeclaredMethod("init");
            init.setAccessible(true);
            init.invoke(minioService);
        }
    }
}