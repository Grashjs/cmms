package com.grash.job;

import com.grash.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteDemoCompaniesJobTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private DeleteDemoCompaniesJob deleteDemoCompaniesJob;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(deleteDemoCompaniesJob, "cloudVersion", true);
    }

    @Test
    void execute_whenNotCloudVersion_doesNotDelete() {
        ReflectionTestUtils.setField(deleteDemoCompaniesJob, "cloudVersion", false);
        JobExecutionContext context = mock(JobExecutionContext.class);

        assertDoesNotThrow(() -> deleteDemoCompaniesJob.execute(context));

        verify(companyRepository, never()).deleteAllByDemoTrue();
    }

    @Test
    void execute_whenCloudVersion_deletesDemoCompanies() {
        JobExecutionContext context = mock(JobExecutionContext.class);

        assertDoesNotThrow(() -> deleteDemoCompaniesJob.execute(context));

        verify(companyRepository).deleteAllByDemoTrue();
    }
}
