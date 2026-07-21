package com.grash.integration;

import com.grash.factory.*;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.*;
import com.grash.service.WorkOrderService;
import com.grash.util.UserTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class WorkOrderServiceIntegrationTest extends MockedServicesTestBase {

    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;

    private Company company;
    private User user;
    @Autowired
    private UserTestUtils userTestUtils;

    @BeforeEach
    void setUp() {
        user = userTestUtils.generateUserAndEnable();
        company = user.getCompany();
    }

    @Test
    void createWorkOrder_shouldPersistAndAssignCustomId() {
        WorkOrder wo = WorkOrderFactory.createWorkOrder();
        WorkOrder saved = workOrderService.create(wo, company);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCustomId()).startsWith("WO");
        assertThat(saved.getTitle()).isEqualTo("Test Work Order");
        assertThat(saved.getStatus()).isEqualTo(Status.OPEN);
        assertThat(saved.getCompany().getId()).isEqualTo(company.getId());
    }

    @Test
    void findById_shouldReturnWorkOrder() {
        WorkOrder wo = WorkOrderFactory.createWorkOrder();
        WorkOrder saved = workOrderService.create(wo, company);

        Optional<WorkOrder> found = workOrderService.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Work Order");
    }

    @Test
    void findById_shouldReturnEmptyForNonexistent() {
        Optional<WorkOrder> found = workOrderService.findById(999999L);
        assertThat(found).isEmpty();
    }

    @Test
    void findByCompany_shouldReturnOnlyWorkOrdersForThatCompany() {
        WorkOrder wo = WorkOrderFactory.createWorkOrder();
        workOrderService.create(wo, company);

        Collection<WorkOrder> results = workOrderService.findByCompany(company.getId());

        assertThat(results).hasSize(1);
        assertThat(results.iterator().next().getCompany().getId()).isEqualTo(company.getId());
    }

    @Test
    void getWorkOrderNumber_shouldGenerateSequentialNumbers() {
        String first = workOrderService.getWorkOrderNumber(company);
        String second = workOrderService.getWorkOrderNumber(company);

        assertThat(first).startsWith("WO");
        assertThat(second).startsWith("WO");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void checkAccessToWorkOrderId_shouldAllowCreator() {
        WorkOrder wo = WorkOrderFactory.createWorkOrderWithCreator(user.getId());
        WorkOrder saved = workOrderService.create(wo, company);

        WorkOrder accessed = workOrderService.checkAccessToWorkOrderId(saved.getId(), user);

        assertThat(accessed.getId()).isEqualTo(saved.getId());
    }

    @Test
    void checkAccessToWorkOrderId_shouldDenyAccessWhenNoPermissions() {
        Role readOnlyRole = roleRepository.save(RoleFactory.createReadOnlyRole());
        User tempReadOnlyUser = userRepository.save(UserFactory.createUser("readonly@example.com", readOnlyRole));
        User readOnlyUser = userRepository.save(tempReadOnlyUser);

        WorkOrder wo = WorkOrderFactory.createWorkOrderWithCreator(user.getId());
        WorkOrder saved = workOrderService.create(wo, company);

        assertThatThrownBy(() -> workOrderService.checkAccessToWorkOrderId(saved.getId(), readOnlyUser))
                .isInstanceOf(com.grash.exception.CustomException.class);
    }

    @Test
    void countUrgent_shouldCountWorkOrdersDueSoon() {
        WorkOrder wo = WorkOrderFactory.createWorkOrder();
        wo.setDueDate(new Date(System.currentTimeMillis() + 12 * 3600 * 1000));
        workOrderService.create(wo, company);

        Integer count = workOrderService.countUrgent(user);

        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void delete_shouldRemoveWorkOrder() {
        WorkOrder wo = WorkOrderFactory.createWorkOrder();
        WorkOrder saved = workOrderService.create(wo, company);

        workOrderService.delete(saved, company);

        assertThat(workOrderService.findById(saved.getId())).isEmpty();
    }
}
