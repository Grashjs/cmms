package com.grash.factory;

import com.grash.model.Company;
import com.grash.model.CompanySettings;

public final class CompanyFactory {

    private CompanyFactory() {
    }

    public static Company createCompany(String name) {
        Company company = new Company();
        company.setName(name);
        company.setEmployeesCount(10);
        company.setCompanySettings(new CompanySettings(company));
        return company;
    }

    public static Company createCompany() {
        return createCompany("Test Company");
    }
}
