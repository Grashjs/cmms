package com.grash.service;

import com.grash.factory.StorageServiceFactory;
import com.grash.model.OwnUser;
import com.grash.utils.CsvFileGenerator;
import com.grash.utils.Helper;
import com.grash.utils.MultipartFileImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncExportService {

    private final WorkOrderService workOrderService;
    private final CsvFileGenerator csvFileGenerator;
    private final StorageServiceFactory storageServiceFactory;
    private final SimpMessageSendingOperations messagingTemplate;

    @Async
    public void exportWorkOrders(OwnUser user, String uuid) {
        try {
            ByteArrayOutputStream target = new ByteArrayOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(target, StandardCharsets.UTF_8);
            csvFileGenerator.writeWorkOrdersToCsv(
                    workOrderService.findByCompanyForExport(user.getCompany().getId()),
                    outputStreamWriter,
                    Helper.getLocale(user),
                    user.getCompany().getCompanySettings().getGeneralPreferences().getCsvSeparator());
            byte[] bytes = target.toByteArray();
            MultipartFile file = new MultipartFileImpl(bytes, "Work Orders.csv");
            String filePath = storageServiceFactory.getStorageService().uploadAndSign(file,
                    user.getCompany().getId() + "/exports/" + uuid + "/work-orders");
            messagingTemplate.convertAndSend("/exports/" + uuid, filePath);
            log.info("Export completed for work-orders, uuid: {}", uuid);
        } catch (Exception e) {
            log.error("Export failed for work-orders, uuid: {}", uuid, e);
            messagingTemplate.convertAndSend("/exports/" + uuid, "error: " + e.getMessage());
        }
    }
}