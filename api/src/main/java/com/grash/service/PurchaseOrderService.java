package com.grash.service;

import com.grash.advancedsearch.SearchCriteria;
import com.grash.advancedsearch.SpecificationBuilder;
import com.grash.dto.PurchaseOrderPatchDTO;
import com.grash.dto.PurchaseOrderShowDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.PurchaseOrderMapper;
import com.grash.model.OwnUser;
import com.grash.model.PurchaseOrder;
import com.grash.model.enums.RoleType;
import com.grash.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final CompanyService companyService;
    private final EntityManager em;

    @Transactional
    public PurchaseOrder create(PurchaseOrder purchaseOrder) {
        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.saveAndFlush(purchaseOrder);
        em.refresh(savedPurchaseOrder);
        return savedPurchaseOrder;
    }

    @Transactional
    public PurchaseOrder update(Long id, PurchaseOrderPatchDTO purchaseOrder) {
        if (purchaseOrderRepository.existsById(id)) {
            PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.findById(id).get();
            PurchaseOrder updatedPurchaseOrder =
                    purchaseOrderRepository.saveAndFlush(purchaseOrderMapper.updatePurchaseOrder(savedPurchaseOrder,
                            purchaseOrder));
            em.refresh(updatedPurchaseOrder);
            return updatedPurchaseOrder;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Collection<PurchaseOrder> getAll() {
        return purchaseOrderRepository.findAll();
    }

    public void delete(Long id) {
        purchaseOrderRepository.deleteById(id);
    }

    public Optional<PurchaseOrder> findById(Long id) {
        return purchaseOrderRepository.findById(id);
    }

    public Collection<PurchaseOrder> findByCompany(Long id) {
        return purchaseOrderRepository.findByCompany_Id(id);
    }

    public PurchaseOrder save(PurchaseOrder purchaseOrder) {
        return purchaseOrderRepository.save(purchaseOrder);
    }

    public boolean isPurchaseOrderInCompany(PurchaseOrder purchaseOrder, long companyId, boolean optional) {
        if (optional) {
            Optional<PurchaseOrder> optionalPurchaseOrder = purchaseOrder == null ? Optional.empty() :
                    findById(purchaseOrder.getId());
            return purchaseOrder == null || (optionalPurchaseOrder.isPresent() && optionalPurchaseOrder.get().getCompany().getId().equals(companyId));
        } else {
            Optional<PurchaseOrder> optionalPurchaseOrder = findById(purchaseOrder.getId());
            return optionalPurchaseOrder.isPresent() && optionalPurchaseOrder.get().getCompany().getId().equals(companyId);
        }
    }

    public Page<PurchaseOrderShowDTO> findBySearchCriteria(SearchCriteria searchCriteria) {
        SpecificationBuilder<PurchaseOrder> builder = new SpecificationBuilder<>();
        searchCriteria.getFilterFields().forEach(builder::with);
        Pageable page = PageRequest.of(searchCriteria.getPageNum(), searchCriteria.getPageSize(),
                searchCriteria.getDirection(), searchCriteria.getSortField());
        return purchaseOrderRepository.findAll(builder.build(), page).map(purchaseOrderMapper::toShowDto);
    }
}
