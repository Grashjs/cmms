package com.grash.job;

import com.grash.model.Company;
import com.grash.repository.CompanyRepository;
import com.grash.repository.UserRepository;
import com.grash.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteDemoCompaniesJob implements Job {

    private final CompanyRepository companyRepository;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Deleting demo companies");
        companyRepository.deleteAllByDemoTrue();
        log.info("Deleted demo companies");
    }
}
