package com.grash.controller.analytics;

import com.grash.dto.analytics.users.UserWOStats;
import com.grash.dto.analytics.users.WOStatsByDay;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.WorkOrder;
import com.grash.model.enums.PermissionEntity;
import com.grash.security.CurrentUser;
import com.grash.service.UserService;
import com.grash.service.WorkOrderService;
import com.grash.utils.Helper;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/analytics/users")
@Api(tags = "UserAnalytics")
@RequiredArgsConstructor
public class UserAnalyticsController {

    private final WorkOrderService workOrderService;
    private final UserService userService;

    @GetMapping("/me/work-orders/overview")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    @Cacheable(
            value = "getUserWOStats",
            key = "#user.id"
    )
    public ResponseEntity<UserWOStats> getWOStats(@ApiIgnore @CurrentUser OwnUser user) {
        Collection<WorkOrder> createdWorkOrders = workOrderService.findByCreatedBy(user.getId());
        Collection<WorkOrder> completedWorkOrders = workOrderService.findByCompletedBy(user.getId());
        return ResponseEntity.ok(UserWOStats.builder()
                .created(createdWorkOrders.size())
                .completed(completedWorkOrders.size())
                .build());
    }

    @GetMapping("/two-weeks/work-orders/{id}")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    public ResponseEntity<List<WOStatsByDay>> getWoStatsByUserFor2Weeks(@PathVariable("id") Long id,
                                                                        @ApiIgnore @CurrentUser OwnUser user) {
        if (user.getRole().getViewPermissions().contains(PermissionEntity.PEOPLE_AND_TEAMS)) {
            Optional<OwnUser> optionalUser = userService.findByIdAndCompany(id, user.getCompany().getId());
            if (optionalUser.isPresent()) {
                Date firstDay = Helper.localDateToDate(LocalDate.now().minusDays(14));
                Collection<WorkOrder> createdWorkOrders = workOrderService.findByCreatedByAndCreatedAtBetween(id,
                        firstDay, new Date());
                Collection<WorkOrder> completedWorkOrders = workOrderService.findByCompletedByAndCreatedAtBetween(id,
                        firstDay, new Date());
                List<WOStatsByDay> result = new ArrayList<>();
                for (int i = 0; i < 14; i++) {
                    Date date = Helper.localDateToDate(LocalDate.now().minusDays(i));
                    int createdWorkOrdersInDay =
                            (int) createdWorkOrders.stream().filter(workOrder -> Helper.isSameDay(workOrder.getCreatedAt(), date)).count();
                    int completedWorkOrdersInDay =
                            (int) completedWorkOrders.stream().filter(workOrder -> Helper.isSameDay(workOrder.getCreatedAt(), date)).count();
                    result.add(WOStatsByDay.builder()
                            .created(createdWorkOrdersInDay)
                            .completed(completedWorkOrdersInDay)
                            .date(date)
                            .build());
                }
                Collections.reverse(result);
                return ResponseEntity.ok(result);
            } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        } else throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
    }
}
