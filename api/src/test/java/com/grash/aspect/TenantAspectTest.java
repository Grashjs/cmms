package com.grash.aspect;

import com.grash.model.abstracts.CompanyAudit;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantAspectTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private TenantAspect tenantAspect;

    @BeforeEach
    void setUp() {
        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
    }

    @AfterEach
    void tearDown() {
        TenantAspect.enableCompanyCheck();
    }

    static class TestController {
        public void create(@RequestBody Object body) {}
        public void createCollection(@RequestBody List<?> bodies) {}
        public void noBody(String param) {}
        public void bodyAndOther(@RequestBody Object body, String other) {}
    }

    static class TestEntity extends CompanyAudit {
        TestEntity() {}
        TestEntity(Long id) { setId(id); }
    }

    static class DtoWithAuditField {
        private TestEntity audit = new TestEntity(5L);
        public TestEntity getAudit() { return audit; }
    }

    static class DtoWithCollectionField {
        private List<TestEntity> audits = List.of(new TestEntity(6L), new TestEntity(7L));
        public List<TestEntity> getAudits() { return audits; }
    }

    static class DtoWithNullIdField {
        private TestEntity audit = new TestEntity();
        public TestEntity getAudit() { return audit; }
    }

    static class DtoWithNonAuditField {
        private String name = "test";
        public String getName() { return name; }
    }

    private void stubMethod(String methodName, Class<?>... paramTypes) throws Exception {
        Method method = TestController.class.getMethod(methodName, paramTypes);
        lenient().when(methodSignature.getMethod()).thenReturn(method);
    }

    @Test
    void ignoreCompanyCheck_disabled_returnsEarly() throws Exception {
        TenantAspect.disableCompanyCheck();
        stubMethod("create", Object.class);

        tenantAspect.validateTenant(joinPoint);

        verifyNoInteractions(entityManager);
    }

    @Test
    void noRequestBodyParameter_doesNothing() throws Exception {
        stubMethod("noBody", String.class);

        tenantAspect.validateTenant(joinPoint);

        verifyNoInteractions(entityManager);
    }

    @Test
    void singleRequestBody_validatesObject() throws Exception {
        stubMethod("create", Object.class);
        DtoWithAuditField dto = new DtoWithAuditField();
        when(joinPoint.getArgs()).thenReturn(new Object[]{dto});

        tenantAspect.validateTenant(joinPoint);

        verify(entityManager).find(TestEntity.class, 5L);
    }

    @Test
    void requestBodyIsCollection_validatesEachElement() throws Exception {
        stubMethod("createCollection", List.class);
        DtoWithAuditField dto1 = new DtoWithAuditField();
        DtoWithAuditField dto2 = new DtoWithAuditField();
        when(joinPoint.getArgs()).thenReturn(new Object[]{List.of(dto1, dto2)});

        tenantAspect.validateTenant(joinPoint);

        verify(entityManager, times(2)).find(TestEntity.class, 5L);
    }

    @Test
    void fieldValueIsCollection_validatesEachElement() throws Exception {
        stubMethod("create", Object.class);
        DtoWithCollectionField dto = new DtoWithCollectionField();
        when(joinPoint.getArgs()).thenReturn(new Object[]{dto});

        tenantAspect.validateTenant(joinPoint);

        verify(entityManager).find(TestEntity.class, 6L);
        verify(entityManager).find(TestEntity.class, 7L);
    }

    @Test
    void companyAuditWithNullId_skipsEntityManagerFind() throws Exception {
        stubMethod("create", Object.class);
        DtoWithNullIdField dto = new DtoWithNullIdField();
        when(joinPoint.getArgs()).thenReturn(new Object[]{dto});

        tenantAspect.validateTenant(joinPoint);

        verify(entityManager, never()).find(any(), any());
    }

    @Test
    void nonCompanyAuditField_doesNothing() throws Exception {
        stubMethod("create", Object.class);
        DtoWithNonAuditField dto = new DtoWithNonAuditField();
        when(joinPoint.getArgs()).thenReturn(new Object[]{dto});

        tenantAspect.validateTenant(joinPoint);

        verifyNoInteractions(entityManager);
    }

    @Test
    void multipleParameters_onlyRequestBodyIsValidated() throws Exception {
        Method method = TestController.class.getMethod("bodyAndOther", Object.class, String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        DtoWithAuditField dto = new DtoWithAuditField();
        when(joinPoint.getArgs()).thenReturn(new Object[]{dto, "other"});

        tenantAspect.validateTenant(joinPoint);

        verify(entityManager).find(TestEntity.class, 5L);
    }

    @Test
    void nullFieldValue_doesNothing() throws Exception {
        stubMethod("create", Object.class);
        Object dto = new Object() {
            private TestEntity audit = null;
        };
        when(joinPoint.getArgs()).thenReturn(new Object[]{dto});

        tenantAspect.validateTenant(joinPoint);

        verify(entityManager, never()).find(any(), any());
    }
}
