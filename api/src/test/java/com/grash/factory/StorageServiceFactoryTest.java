package com.grash.factory;

import com.grash.model.enums.StorageType;
import com.grash.service.GCPService;
import com.grash.service.MinioService;
import com.grash.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
class StorageServiceFactoryTest {

    @Mock
    private GCPService gcpService;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private StorageServiceFactory storageServiceFactory;

    private void setStorageType(StorageType storageType) {
        ReflectionTestUtils.setField(storageServiceFactory, "storageType", storageType);
    }

    @Test
    void getStorageService_gcp_returnsGcpService() {
        setStorageType(StorageType.GCP);

        StorageService result = storageServiceFactory.getStorageService();

        assertSame(gcpService, result);
    }

    @Test
    void getStorageService_minio_returnsMinioService() {
        setStorageType(StorageType.MINIO);

        StorageService result = storageServiceFactory.getStorageService();

        assertSame(minioService, result);
    }
}
