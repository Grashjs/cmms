package com.grash.service;

import com.grash.model.OwnUser;
import com.grash.model.WorkOrder;
import com.grash.model.WorkOrderHistory;
import com.grash.repository.WorkOrderAudRepository;
import com.grash.repository.WorkOrderHistoryRepository;
import com.grash.repository.WorkOrderRepository;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkOrderHistoryService {
    private final WorkOrderHistoryRepository workOrderHistoryRepository;
    private final WorkOrderAudRepository workOrderAudRepository;
    private final WorkOrderRepository workOrderRepository;
    private final MessageSource messageSource;

    public WorkOrderHistory create(WorkOrderHistory workOrderHistory) {
        return workOrderHistoryRepository.save(workOrderHistory);
    }

    public WorkOrderHistory update(WorkOrderHistory workOrderHistory) {
        return workOrderHistoryRepository.save(workOrderHistory);
    }

    public Collection<WorkOrderHistory> getAll() {
        return workOrderHistoryRepository.findAll();
    }

    public void delete(Long id) {
        workOrderHistoryRepository.deleteById(id);
    }

    public Optional<WorkOrderHistory> findById(Long id) {
        return workOrderHistoryRepository.findById(id);
    }

    public Collection<WorkOrderHistory> findByWorkOrder(Long id) {
        return workOrderAudRepository.findByIdAndRevtype(id, 1).stream().map(workOrderAud -> {
            WorkOrder workOrder = workOrderRepository.findById(id).get();
            OwnUser user = workOrderAud.getWorkOrderAudId().getRev().getUser();
            return WorkOrderHistory.builder()
                    .workOrder(workOrder)
                    .name(workOrderAud.getSummary(messageSource, Helper.getLocale(user)))
                    .user(user)
                    .build();
        }).collect(Collectors.toList());
    }
}
