package com.grash.storage;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StorageMetadataMigrationTest {
    @Test
    void gcpDryRunIsReadOnlyAndApplyUsesBothPreconditions() {
        Storage storage = mock(Storage.class);
        Page<Blob> page = mock(Page.class);
        Blob old = mock(Blob.class);
        when(old.getName()).thenReturn("report.html");
        when(old.getContentType()).thenReturn("text/html");
        when(page.iterateAll()).thenReturn(List.of(old));
        when(storage.list(eq("bucket"), any(Storage.BlobListOption[].class))).thenReturn(page);
        assertThat(StorageMetadataMigration.gcp(storage, "bucket", "", false))
                .isEqualTo(new StorageMetadataMigration.Result(1, 1, false));
        verify(old, never()).toBuilder();
        verify(storage, never()).update(any(BlobInfo.class), any(Storage.BlobTargetOption[].class));
        Blob.Builder builder = mock(Blob.Builder.class, RETURNS_SELF);
        when(old.toBuilder()).thenReturn(builder);
        when(builder.build()).thenReturn(old);
        assertThat(StorageMetadataMigration.gcp(storage, "bucket", "", true).changed()).isEqualTo(1);
        verify(builder).setContentType("application/octet-stream");
        verify(builder).setContentDisposition("attachment; filename=\"report.html\"");
        verify(builder).setCacheControl("private, no-store");
        verify(builder).setContentEncoding(null);
        verify(storage).update(old, Storage.BlobTargetOption.generationMatch(), Storage.BlobTargetOption.metagenerationMatch());
    }

    @Test
    void gcpSafeMetadataIsIdempotent() {
        Storage storage = mock(Storage.class);
        Page<Blob> page = mock(Page.class);
        Blob safe = mock(Blob.class);
        when(safe.getName()).thenReturn("image.png");
        when(safe.getContentType()).thenReturn(AttachmentPolicy.DOWNLOAD_TYPE);
        when(safe.getContentDisposition()).thenReturn(AttachmentPolicy.disposition("image.png", false));
        when(safe.getCacheControl()).thenReturn(AttachmentPolicy.CACHE_CONTROL);
        when(page.iterateAll()).thenReturn(List.of(safe));
        when(storage.list(eq("bucket"), any(Storage.BlobListOption[].class))).thenReturn(page);
        assertThat(StorageMetadataMigration.gcp(storage, "bucket", "", true).changed()).isZero();
        verify(storage, never()).update(any(BlobInfo.class), any(Storage.BlobTargetOption[].class));
    }
}
