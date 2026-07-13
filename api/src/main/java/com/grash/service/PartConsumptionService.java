package com.grash.service;

import com.grash.model.OwnUser;
import com.grash.model.PartConsumption;
import com.grash.model.enums.RoleType;
import com.grash.repository.PartConsumptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PartConsumptionService {
    private final PartConsumptionRepository partConsumptionRepository;

    public PartConsumption create(PartConsumption PartConsumption) {
        return partConsumptionRepository.save(PartConsumption);
    }

    public Collection<PartConsumption> getAll() {
        return partConsumptionRepository.findAll();
    }

    public void delete(Long id) {
        partConsumptionRepository.deleteById(id);
    }

    public Optional<PartConsumption> findById(Long id) {
        return partConsumptionRepository.findById(id);
    }

    public Collection<PartConsumption> findByCompany(Long id) {
        return partConsumptionRepository.findByCompany_Id(id);
    }

    public Collection<PartConsumption> findByCreatedAtBetweenAndCompany(Date date1, Date date2, Long id) {
        return partConsumptionRepository.findByCreatedAtBetweenAndCompany_Id(date1, date2, id);
    }

    public Collection<PartConsumption> findByWorkOrderAndPart(Long workOrderId, Long partId) {
        return partConsumptionRepository.findByWorkOrder_IdAndPart_Id(workOrderId, partId);
    }

    public void save(PartConsumption partConsumption) {
        partConsumptionRepository.save(partConsumption);
    }

    public Collection<PartConsumption> findByCompanyAndCreatedAtBetween(Long id, Date start, Date end) {
        return partConsumptionRepository.findByCompany_IdAndCreatedAtBetween(id, start, end);
    }

    public List<PartConsumption> findByWorkOrders(List<Long> ids) {
        return partConsumptionRepository.findByWorkOrder_IdIn(ids);
    }
}
