package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.configuration.MobileOfflineProperties;
import com.grash.dto.offline.*;
import com.grash.exception.CustomException;
import com.grash.factory.StorageServiceFactory;
import com.grash.mapper.TaskMapper;
import com.grash.model.Company;
import com.grash.model.File;
import com.grash.model.User;
import com.grash.model.enums.FileType;
import com.grash.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileOfflineFileServiceTest {
    @Mock MobileOfflineProperties properties;
    @Mock MobileOfflineRepository ledger;
    @Mock MobileOfflineAccessService access;
    @Mock FileService filePermissions;
    @Mock FileRepository fileRepository;
    @Mock StorageServiceFactory storageFactory;
    @Mock StorageService storage;
    @Mock TaskRepository tasks;
    @Mock TaskService taskService;
    @Mock EntityManager em;
    ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    MobileOfflineFileService files;
    MobileOfflineService offline;
    User user;
    Map<UUID, MobileOfflineRepository.Receipt> receipts;

    @BeforeEach void fixture() {
        Company company = new Company(); company.setId(52L);
        user = new User(); user.setId(34L); user.setCompany(company);
        files = new MobileOfflineFileService(filePermissions, fileRepository, storageFactory, access,
                tasks, taskService, ledger, json, em);
        offline = new MobileOfflineService(properties, ledger, mock(MobileOfflineSnapshotService.class), access,
                tasks, taskService, mock(TaskMapper.class), json, mock(CommentService.class), mock(CommentRepository.class),
                files, mock(WorkOrderService.class), mock(ApplicationEventPublisher.class), mock(LaborService.class), mock(LaborRepository.class));
        receipts = new HashMap<>();
        lenient().when(properties.isEligible(user)).thenReturn(true);
        lenient().when(access.existsInCompany(10, user)).thenReturn(true);
        lenient().when(access.canView(10, user)).thenReturn(true);
        lenient().when(access.canEdit(10, user)).thenReturn(true);
        lenient().when(storageFactory.getStorageService()).thenReturn(storage);
        lenient().when(fileRepository.saveAndFlush(any())).thenAnswer(call -> {
            File file = call.getArgument(0); file.setId(123L); return file;
        });
        lenient().when(ledger.reserveOperation(eq(52L), eq(34L), any(), eq(10L), anyString(), any())).thenAnswer(call -> {
            UUID id = call.getArgument(2);
            if (receipts.containsKey(id)) return false;
            receipts.put(id, new MobileOfflineRepository.Receipt(id, 10, call.getArgument(4), null));
            return true;
        });
        lenient().doAnswer(call -> {
            UUID id = call.getArgument(2); var old = receipts.get(id);
            receipts.put(id, new MobileOfflineRepository.Receipt(id, 10, old.payloadHash(), call.getArgument(3)));
            return null;
        }).when(ledger).finishOperation(eq(52L), eq(34L), any(), anyString());
        lenient().when(ledger.receipt(eq(52L), eq(34L), any())).thenAnswer(call -> Optional.ofNullable(receipts.get(call.getArgument(2))));
    }

    byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }
    MobileOfflineFileUpload transfer(UUID id, String text) throws Exception {
        byte[] data = bytes(text);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        return new MobileOfflineFileUpload(id, 52L, 34L, 10L, MobileOfflineFileUpload.Target.WORK_ORDER,
                null, "field.pdf", "application/pdf", (long) data.length, digest, FileType.OTHER);
    }
    MobileOfflineResult upload(MobileOfflineFileUpload metadata, String text) {
        return offline.uploadFile(metadata.operationId(), metadata,
                new MockMultipartFile("file", "copy", "application/octet-stream", bytes(text)), user);
    }

    @Test void successfulReplayReturnsReceiptWithoutWritingStorageOrFileAgain() throws Exception {
        var metadata = transfer(UUID.randomUUID(), "document");
        var applied = upload(metadata, "document");
        File file = new File(); file.setId(123L);
        when(fileRepository.findByIdAndCompany_Id(123L, 52L)).thenReturn(Optional.of(file));
        var replay = upload(metadata, "document");
        assertEquals(applied, replay); assertEquals(123L, replay.fileId());
        verify(storage, times(1)).uploadAt(org.mockito.AdditionalMatchers.aryEq(bytes("document")), contains(metadata.sha256()), eq("application/pdf"));
        verify(fileRepository, times(1)).saveAndFlush(any());
        verify(filePermissions, times(1)).consumeUpload(user, false);
    }

    @Test void sameUuidWithDifferentActualBytesNeverOverwritesAcceptedObject() throws Exception {
        var metadata = transfer(UUID.randomUUID(), "first"); upload(metadata, "first");
        var error = assertThrows(CustomException.class, () -> upload(metadata, "other"));
        assertEquals("OPERATION_ID_REUSED", error.getMessage());
        verify(storage, times(1)).uploadAt(any(), anyString(), anyString());
        verify(fileRepository, times(1)).saveAndFlush(any());
    }

    @Test void dishonestSizeAndHashProduceDurableRejectionsBeforeAnyStorageWrite() throws Exception {
        var wrongHash = transfer(UUID.randomUUID(), "first");
        var result = upload(wrongHash, "other"); // Same length: this specifically requires the actual digest.
        assertEquals(MobileOfflineResult.Status.REJECTED, result.status());
        assertEquals("INVALID_FILE_CONTENT", result.code());
        assertNotNull(receipts.get(wrongHash.operationId()).resultJson());
        var wrongSize = transfer(UUID.randomUUID(), "short");
        assertEquals("INVALID_FILE_CONTENT", upload(wrongSize, "longer document").code());
        verifyNoInteractions(storage, fileRepository);
    }

    @Test void ownerMismatchAndRevokedTargetCannotWriteAnObject() throws Exception {
        var valid = transfer(UUID.randomUUID(), "doc");
        var other = new MobileOfflineFileUpload(valid.operationId(), 52L, 35L, 10L, valid.target(), null,
                valid.name(), valid.mimeType(), valid.size(), valid.sha256(), valid.fileType());
        assertEquals("OFFLINE_CONTEXT_MISMATCH", assertThrows(CustomException.class, () -> upload(other, "doc")).getMessage());
        when(access.canEdit(10, user)).thenReturn(false);
        assertEquals("ACCESS_REVOKED", upload(valid, "doc").code());
        verifyNoInteractions(storage, fileRepository);
    }

    @Test void rolledBackReceiptCannotMakeAnotherPayloadOverwriteTheOrphanObject() throws Exception {
        UUID id = UUID.randomUUID(); var first = transfer(id, "first"); upload(first, "first");
        receipts.clear(); // Models SQL rollback after object upload; object storage retains its old bytes.
        var second = transfer(id, "second"); upload(second, "second");
        var paths = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(storage, times(2)).uploadAt(any(), paths.capture(), anyString());
        assertNotEquals(paths.getAllValues().get(0), paths.getAllValues().get(1));
    }

    @Test void unknownRejectedAndWrongTargetTransfersCannotBeAttached() throws Exception {
        UUID unknown = UUID.randomUUID();
        assertEquals("TRANSFER_NOT_CONFIRMED", files.resolve(List.of(unknown), MobileOfflineFileUpload.Target.WORK_ORDER, 10, null, user).error());
        var metadata = transfer(UUID.randomUUID(), "first"); upload(metadata, "other");
        assertEquals("TRANSFER_REJECTED", files.resolve(List.of(metadata.operationId()), MobileOfflineFileUpload.Target.WORK_ORDER, 10, null, user).error());
        var successful = transfer(UUID.randomUUID(), "valid"); upload(successful, "valid");
        assertEquals("INVALID_FILE_TARGET", files.resolve(List.of(successful.operationId()), MobileOfflineFileUpload.Target.COMMENT, 10, null, user).error());
    }
}
