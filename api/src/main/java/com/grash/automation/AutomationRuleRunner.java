package com.grash.automation;

import com.grash.automation.action.ActionHandler;
import com.grash.automation.event.EntityChangedEvent;
import com.grash.automation.eval.ExecutionContext;
import com.grash.automation.eval.RuleEvaluator;
import com.grash.automation.model.ActionType;
import com.grash.automation.model.AutomationActionStep;
import com.grash.automation.model.AutomationRule;
import com.grash.automation.repository.AutomationRuleRepository;
import com.grash.exception.CustomException;
import com.grash.service.AssetService;
import com.grash.service.CompanyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Runs one rule against one event, in its own transaction.
 *
 * <p>Own transaction so that one rule cannot take another down with it, and so that a rolled back
 * action leaves the run log — written elsewhere, on purpose — intact.
 */
@Slf4j
@Service
public class AutomationRuleRunner {

    private final AutomationRuleRepository ruleRepository;
    private final AutomationRunService runService;
    private final RuleEvaluator evaluator;
    private final CompanyService companyService;
    private final AssetService assetService;
    private final Map<ActionType, ActionHandler> handlers = new EnumMap<>(ActionType.class);

    @Value("${automation.max-depth:3}")
    private int defaultMaxDepth;

    public AutomationRuleRunner(AutomationRuleRepository ruleRepository,
                                AutomationRunService runService,
                                RuleEvaluator evaluator,
                                CompanyService companyService,
                                AssetService assetService,
                                List<ActionHandler> actionHandlers) {
        this.ruleRepository = ruleRepository;
        this.runService = runService;
        this.evaluator = evaluator;
        this.companyService = companyService;
        this.assetService = assetService;
        actionHandlers.forEach(handler -> this.handlers.put(handler.getType(), handler));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RunOutcome run(Long ruleId, EntityChangedEvent event) {
        AutomationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new CustomException("Rule " + ruleId + " disappeared", HttpStatus.NOT_FOUND));

        int maxDepth = rule.getMaxDepth() == null ? defaultMaxDepth : rule.getMaxDepth();
        if (event.depth() >= maxDepth) {
            return RunOutcome.skipped("Cascade depth " + event.depth() + " reached the limit of " + maxDepth);
        }
        if (runService.alreadyRanInThisCascade(ruleId, event)) {
            return RunOutcome.skipped("Already ran for this entity in cascade " + event.correlationId());
        }

        ExecutionContext context = new ExecutionContext(
                event,
                companyService.findById(event.companyId())
                        .orElseThrow(() -> new CustomException("Company " + event.companyId() + " not found",
                                HttpStatus.NOT_FOUND)),
                loadTriggerEntity(event));

        String unmet = evaluator.firstUnmetCondition(rule, context);
        if (unmet != null) {
            return RunOutcome.skipped("Condition not met: " + unmet);
        }

        int executed = 0;
        for (AutomationActionStep step : rule.getActions()) {
            try {
                handlerFor(step).execute(step, context);
                executed++;
            } catch (Exception exception) {
                String message = step.getActionType() + " failed: " + exception.getMessage();
                if (step.isAbortOnFailure()) {
                    return RunOutcome.failed(message, executed);
                }
                log.warn("Rule {} step {} failed but is configured to continue", ruleId,
                        step.getActionType(), exception);
            }
        }
        return RunOutcome.success(executed);
    }

    /**
     * Loaded fresh, by id, inside this transaction. The event carries no entity for exactly this
     * reason: by the time a listener runs, anything it was handed would be detached and possibly
     * stale.
     */
    private Object loadTriggerEntity(EntityChangedEvent event) {
        return switch (event.entityType()) {
            case ASSET -> assetService.findById(event.entityId())
                    .orElseThrow(() -> new CustomException("Asset " + event.entityId() + " not found",
                            HttpStatus.NOT_FOUND));
            // Not a silent skip. These types are storable on a rule but nothing publishes them
            // yet; a rule configured against one must say so instead of never firing.
            default -> throw new CustomException("Trigger entity type " + event.entityType()
                    + " is not wired up yet", HttpStatus.NOT_IMPLEMENTED);
        };
    }

    private ActionHandler handlerFor(AutomationActionStep step) {
        ActionHandler handler = handlers.get(step.getActionType());
        if (handler == null) {
            throw new CustomException("No handler for action " + step.getActionType(),
                    HttpStatus.NOT_IMPLEMENTED);
        }
        return handler;
    }
}
