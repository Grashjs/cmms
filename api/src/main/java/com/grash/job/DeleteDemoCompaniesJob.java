package com.grash.job;

import com.grash.model.Company;
import com.grash.repository.CompanyRepository;
import com.grash.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeleteDemoCompaniesJob implements Job {

    private final CompanyRepository companyRepository;
    private final CompanyService companyService;

    @Override
    public void execute(JobExecutionContext context) {
        List<Company> companies = companyRepository.findByDemoTrue();
        companies.forEach(company -> companyService.delete(company.getId()));
    }
}
