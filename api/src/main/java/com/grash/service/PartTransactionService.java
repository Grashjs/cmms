package com.grash.service;

import com.grash.model.PartTransaction;
import com.grash.repository.PartTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PartTransactionService {
    private final PartTransactionRepository partTransactionRepository;

    public PartTransaction create(PartTransaction PartTransaction) {
        return partTransactionRepository.save(PartTransaction);
    }

    public Collection<PartTransaction> getAll() {
        return partTransactionRepository.findAll();
    }

    public void delete(Long id) {
        partTransactionRepository.deleteById(id);
    }

    public Optional<PartTransaction> findById(Long id) {
        return partTransactionRepository.findById(id);
    }

    public Collection<PartTransaction> findByCompany(Long id) {
        return partTransactionRepository.findByCompany_Id(id);
    }

    public Collection<PartTransaction> findByWorkOrderAndPart(Long workOrderId, Long partId) {
        return partTransactionRepository.findByWorkOrder_IdAndPart_Id(workOrderId, partId);
    }

    public void save(PartTransaction partTransaction) {
        partTransactionRepository.save(partTransaction);
    }

    public Collection<PartTransaction> findConsumptionsByCompanyAndCreatedAtBetween(Long id, Date start, Date end) {
        return partTransactionRepository.findConsumptionsByCompanyBetween(id, start, end);
    }
    
    public List<Object[]> findTopNAssetsByConsumption(Long companyId, Date start, Date end, int limit) {
        return partTransactionRepository.findTopNAssetsByConsumption(companyId, start, end, limit);
    }

    public Page<PartTransaction> findByCompanyForExport(Long companyId, Pageable pageable) {
        return partTransactionRepository.findByCompanyForExport(companyId, pageable);
    }
}
