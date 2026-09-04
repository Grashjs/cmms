package com.grash.automation;

import com.grash.automation.action.ActionHandler;
import com.grash.automation.action.ActionParameters;
import com.grash.automation.dto.AutomationMetaDTO;
import com.grash.automation.event.ChangeType;
import com.grash.automation.event.EntityType;
import com.grash.automation.eval.OperandDescriptor;
import com.grash.automation.eval.OperandResolver;
import com.grash.model.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles the editor's metadata by asking the components themselves.
 *
 * <p>Nothing here enumerates subjects or actions: the resolvers and handlers Spring found are the
 * answer. Adding a resolver therefore adds a condition to the editor, and adding a handler adds
 * an action, with no second list to update — which is the whole reason this layer exists.
 */
@Service
@RequiredArgsConstructor
public class AutomationMetaService {

    private final List<OperandResolver> resolvers;
    private final List<ActionHandler> handlers;

    @Value("${automation.enabled:false}")
    private boolean engineEnabled;

    /**
     * The triggers that are wired end to end, i.e. that some service actually publishes.
     *
     * <p>This one list is hand-maintained and cannot be derived — a publisher is a line inside a
     * domain service, not a bean to enumerate — so it is the one place that can go stale. It is
     * kept anyway, because the alternative is worse: without it the editor offers every
     * {@link EntityType} × {@link ChangeType} combination, and 35 of the 36 produce a rule that
     * saves, looks correct and never fires. <b>When a publish point is added, add it here.</b>
     *
     * @see com.grash.service.AssetService the only publisher so far
     */
    private static final List<AutomationMetaDTO.Trigger> LIVE_TRIGGERS = List.of(
            new AutomationMetaDTO.Trigger(EntityType.ASSET, ChangeType.UPDATED, true,
                    // The diff reports the status change only. AssetService.update builds its own
                    // field list for the webhook and status is not in it, so the event is
                    // published from the status-change path instead — see the concept document.
                    List.of("status")));

    @Transactional(readOnly = true)
    public AutomationMetaDTO describe(Company company) {
        List<OperandDescriptor> subjects = new ArrayList<>();
        for (OperandResolver resolver : resolvers) {
            subjects.addAll(resolver.describe(company));
        }

        return new AutomationMetaDTO(
                engineEnabled,
                allTriggers(),
                subjects,
                handlers.stream().map(ActionHandler::descriptor).toList(),
                ActionParameters.PLACEHOLDERS.keySet().stream().sorted()
                        .map(name -> "${" + name + "}").toList());
    }

    /**
     * Every combination, each marked live or not. The dead ones are reported rather than omitted
     * so the editor can say "not yet available" instead of leaving the user to guess why the
     * trigger they expected is missing.
     */
    private List<AutomationMetaDTO.Trigger> allTriggers() {
        List<AutomationMetaDTO.Trigger> triggers = new ArrayList<>();
        for (EntityType entityType : EntityType.values()) {
            for (ChangeType changeType : ChangeType.values()) {
                triggers.add(LIVE_TRIGGERS.stream()
                        .filter(live -> live.entityType() == entityType && live.changeType() == changeType)
                        .findFirst()
                        .orElseGet(() -> new AutomationMetaDTO.Trigger(entityType, changeType, false,
                                List.of())));
            }
        }
        return triggers;
    }
}
