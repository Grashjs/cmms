package com.grash.automation;

import com.grash.automation.action.ActionHandler;
import com.grash.automation.dto.AutomationActionPostDTO;
import com.grash.automation.dto.AutomationConditionPostDTO;
import com.grash.automation.dto.AutomationRulePostDTO;
import com.grash.automation.eval.CustomFieldResolver;
import com.grash.automation.eval.OperandResolver;
import com.grash.automation.model.ActionType;
import com.grash.automation.model.AutomationActionStep;
import com.grash.automation.model.AutomationCondition;
import com.grash.automation.model.AutomationRule;
import com.grash.automation.repository.AutomationRuleRepository;
import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.CustomField;
import com.grash.repository.CustomFieldRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Creates, updates and validates rules.
 *
 * <p>The validation is the interesting part. Everything the old engine let you configure and then
 * ignored — a condition nothing evaluates, an action nothing carries out — is refused here, at
 * save time, with a message. A rule that saves is a rule that runs.
 */
@Service
@RequiredArgsConstructor
public class AutomationRuleService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AutomationRuleRepository ruleRepository;
    private final CustomFieldRepository customFieldRepository;
    private final List<OperandResolver> resolvers;
    private final List<ActionHandler> handlers;

    @Transactional
    public AutomationRule create(AutomationRulePostDTO dto, Company company) {
        AutomationRule rule = new AutomationRule();
        rule.setCompany(company);
        apply(dto, rule, company);
        return ruleRepository.save(rule);
    }

    /**
     * Replaces the rule's contents in place. Not delete-and-recreate: the run log points at the
     * rule id, and the old engine's habit of handing out a new id on every edit would orphan the
     * whole history at each save.
     */
    @Transactional
    public AutomationRule update(Long id, AutomationRulePostDTO dto, Company company) {
        AutomationRule rule = findByIdAndCompany(id, company.getId())
                .orElseThrow(() -> new CustomException("Rule not found", HttpStatus.NOT_FOUND));
        rule.getConditions().clear();
        rule.getActions().clear();
        apply(dto, rule, company);
        return ruleRepository.save(rule);
    }

    @Transactional
    public AutomationRule setEnabled(Long id, boolean enabled, Company company) {
        AutomationRule rule = findByIdAndCompany(id, company.getId())
                .orElseThrow(() -> new CustomException("Rule not found", HttpStatus.NOT_FOUND));
        rule.setEnabled(enabled);
        return ruleRepository.save(rule);
    }

    @Transactional
    public void delete(Long id, Company company) {
        AutomationRule rule = findByIdAndCompany(id, company.getId())
                .orElseThrow(() -> new CustomException("Rule not found", HttpStatus.NOT_FOUND));
        ruleRepository.delete(rule);
    }

    @Transactional(readOnly = true)
    public Collection<AutomationRule> findByCompany(Long companyId) {
        return ruleRepository.findByCompany_Id(companyId);
    }

    @Transactional(readOnly = true)
    public Optional<AutomationRule> findByIdAndCompany(Long id, Long companyId) {
        return ruleRepository.findById(id)
                .filter(rule -> rule.getCompany().getId().equals(companyId));
    }

    private void apply(AutomationRulePostDTO dto, AutomationRule rule, Company company) {
        rule.setTitle(dto.title());
        rule.setTriggerChangeType(dto.triggerChangeType());
        rule.setTriggerEntityType(dto.triggerEntityType());
        rule.setTriggerChangedFields(dto.triggerChangedFields() == null
                ? new HashSet<>() : new HashSet<>(dto.triggerChangedFields()));
        rule.setEnabled(dto.enabled() == null || dto.enabled());
        rule.setMaxDepth(dto.maxDepth());

        if (dto.conditions() != null) {
            for (AutomationConditionPostDTO conditionDto : dto.conditions()) {
                rule.addCondition(toCondition(conditionDto, company));
            }
        }
        int index = 0;
        for (AutomationActionPostDTO actionDto : dto.actions()) {
            rule.addAction(toAction(actionDto, index++));
        }
    }

    private AutomationCondition toCondition(AutomationConditionPostDTO dto, Company company) {
        assertResolvable(dto.subject());

        AutomationCondition condition = new AutomationCondition();
        condition.setSubject(dto.subject());
        condition.setOperator(dto.operator());
        condition.setExpectedValue(dto.expectedValue());

        boolean isCustomField = CustomFieldResolver.SUBJECT.equals(dto.subject());
        if (isCustomField) {
            condition.setCustomField(loadCustomField(dto.customFieldId(), company));
        } else if (dto.customFieldId() != null) {
            throw new CustomException("customFieldId only applies to subject \""
                    + CustomFieldResolver.SUBJECT + "\"", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return condition;
    }

    private void assertResolvable(String subject) {
        if (resolvers.stream().noneMatch(resolver -> resolver.supports(subject))) {
            throw new CustomException("Nothing can read the subject \"" + subject + "\". "
                    + "A condition that cannot be evaluated is refused rather than stored, "
                    + "because a stored one would silently disable the whole rule.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private CustomField loadCustomField(Long customFieldId, Company company) {
        if (customFieldId == null) {
            throw new CustomException("A condition on \"" + CustomFieldResolver.SUBJECT
                    + "\" needs customFieldId", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        CustomField field = customFieldRepository.findById(customFieldId)
                .orElseThrow(() -> new CustomException("Custom field " + customFieldId + " not found",
                        HttpStatus.NOT_FOUND));
        Long owner = field.getCompanySettings() == null || field.getCompanySettings().getCompany() == null
                ? null : field.getCompanySettings().getCompany().getId();
        if (owner == null || !owner.equals(company.getId())) {
            throw new CustomException("Custom field " + customFieldId + " belongs to another company",
                    HttpStatus.FORBIDDEN);
        }
        return field;
    }

    private AutomationActionStep toAction(AutomationActionPostDTO dto, int index) {
        Set<ActionType> supported = handlers.stream().map(ActionHandler::getType)
                .collect(java.util.stream.Collectors.toSet());
        if (!supported.contains(dto.actionType())) {
            throw new CustomException("No handler for action " + dto.actionType() + ". Available: "
                    + supported, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        assertJsonObject(dto.parameters());

        AutomationActionStep step = new AutomationActionStep();
        step.setActionType(dto.actionType());
        step.setParameters(dto.parameters());
        step.setOrderIndex(dto.orderIndex() == null ? index : dto.orderIndex());
        step.setAbortOnFailure(dto.abortOnFailure() == null || dto.abortOnFailure());
        return step;
    }

    /**
     * Syntax only for now. Checking each parameter against the handler's descriptor is what the
     * metadata endpoint brings in the next phase; until then a typo in a key surfaces as a FAILED
     * run rather than a rejected save, which the run log does say out loud.
     */
    private void assertJsonObject(String parameters) {
        if (parameters == null || parameters.isBlank()) {
            return;
        }
        try {
            if (!MAPPER.readTree(parameters).isObject()) {
                throw new CustomException("Action parameters must be a JSON object",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } catch (CustomException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CustomException("Action parameters are not valid JSON: " + exception.getMessage(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
