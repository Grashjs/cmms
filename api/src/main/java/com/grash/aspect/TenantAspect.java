package com.grash.aspect;

import com.grash.model.abstracts.CompanyAudit;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.persistence.EntityManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static org.apache.commons.lang3.reflect.FieldUtils.getAllFields;

@Aspect
@Component
@RequiredArgsConstructor
public class TenantAspect {

    private final EntityManager entityManager;
    private static final ThreadLocal<Boolean> ignoreCompanyCheck = ThreadLocal.withInitial(() -> false);

    public static void disableCompanyCheck() {
        ignoreCompanyCheck.set(true);
    }

    public static void enableCompanyCheck() {
        ignoreCompanyCheck.set(false);
    }

    @Before("@annotation(org.springframework.web.bind.annotation.PostMapping) || @annotation(org.springframework.web" +
            ".bind.annotation.PatchMapping)")
    public void validateTenant(JoinPoint joinPoint) {
        if (ignoreCompanyCheck.get()) return;
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                Object arg = joinPoint.getArgs()[i]; // Get the requestBody
                if (arg instanceof List) {
                    List<?> list = (List<?>) arg;
                    list.forEach(this::validateObject);
                } else {
                    validateObject(arg);
                }
            }
        }
    }

    private void validateObject(Object obj) {
        Field[] fields = getAllFields(obj.getClass());
        for (Field field : fields) {
            field.setAccessible(true);
            Object fieldValue = null;
            try {
                fieldValue = field.get(obj); // Get the value of the field inside request body
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (fieldValue instanceof List) {
                List<?> list = (List<?>) fieldValue;
                list.forEach(this::validateFieldElement);
            } else {
                validateFieldElement(fieldValue);
            }
        }
    }

    private void validateFieldElement(Object object) {
        if (object instanceof CompanyAudit) {
            CompanyAudit companyAudit = (CompanyAudit) object;
            if (companyAudit.getId() == null) return;
            entityManager.find(object.getClass(), companyAudit.getId()); // should fail here if from other company
            // because of @PostLoad
        }
    }
}

