package com.grash.service;

import com.grash.dto.RequestApproveDTO;
import com.grash.mapper.WorkflowMapper;
import com.grash.model.Company;
import com.grash.model.Location;
import com.grash.model.Request;
import com.grash.model.User;
import com.grash.model.Workflow;
import com.grash.model.WorkflowAction;
import com.grash.model.WorkflowCondition;
import com.grash.model.enums.workflow.RequestAction;
import com.grash.model.enums.workflow.RequestCondition;
import com.grash.repository.AssetRepository;
import com.grash.repository.PurchaseOrderRepository;
import com.grash.repository.RequestRepository;
import com.grash.repository.UserRepository;
import com.grash.repository.WorkOrderRepository;
import com.grash.repository.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @InjectMocks
    private WorkflowService workflowService;

    @Mock
    private WorkflowRepository workflowRepository;
    @Mock
    private WorkflowMapper workflowMapper;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private RequestRepository requestRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RequestService requestService;

    @BeforeEach
    void setUp() {
        workflowService.setRequestService(requestService);
    }

    private Location location(Long id, Location parent) {
        Location location = new Location();
        location.setId(id);
        location.setParentLocation(parent);
        return location;
    }

    private Workflow buildApproveWorkflow(Location conditionLocation, Company company) {
        WorkflowCondition condition = WorkflowCondition.builder()
                .requestCondition(RequestCondition.LOCATION_IS)
                .location(conditionLocation)
                .build();
        WorkflowAction action = WorkflowAction.builder()
                .requestAction(RequestAction.APPROVE)
                .build();
        Workflow workflow = new Workflow();
        workflow.setCompany(company);
        workflow.setSecondaryConditions(new ArrayList<>(List.of(condition)));
        workflow.setAction(action);
        workflow.setEnabled(true);
        return workflow;
    }

    @Test
    void runRequest_locationConditionOnParent_approvesRequestInChildLocation() {
        Location campus = location(1L, null);
        Location department = location(2L, campus);
        Location room = location(3L, department);
        Request request = new Request();
        request.setId(10L);
        request.setLocation(room);

        Company company = mock(Company.class);
        when(company.getId()).thenReturn(1L);
        User owner = new User();
        owner.setId(2L);
        when(userRepository.findCompanyOwner(1L)).thenReturn(Optional.of(owner));

        workflowService.runRequest(buildApproveWorkflow(campus, company), request);

        verify(requestService).approveFromWorkflow(eq(10L), any(RequestApproveDTO.class), eq(owner));
    }

    @Test
    void runRequest_locationConditionOnUnrelatedLocation_doesNotApprove() {
        Location campus = location(1L, null);
        Location unrelated = location(4L, null);
        Request request = new Request();
        request.setId(10L);
        request.setLocation(unrelated);

        Company company = mock(Company.class);

        workflowService.runRequest(buildApproveWorkflow(campus, company), request);

        verify(requestService, never()).approveFromWorkflow(any(), any(), any());
    }

    @Test
    void runRequest_specificRoomCondition_approvesRequestInThatRoom() {
        Location room = location(3L, null);
        Request request = new Request();
        request.setId(10L);
        request.setLocation(room);

        Company company = mock(Company.class);
        when(company.getId()).thenReturn(1L);
        User owner = new User();
        owner.setId(2L);
        when(userRepository.findCompanyOwner(1L)).thenReturn(Optional.of(owner));

        workflowService.runRequest(buildApproveWorkflow(room, company), request);

        verify(requestService).approveFromWorkflow(eq(10L), any(RequestApproveDTO.class), eq(owner));
    }
}
