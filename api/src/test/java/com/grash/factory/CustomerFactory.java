package com.grash.factory;

import com.grash.model.Customer;

public final class CustomerFactory {

    private CustomerFactory() {
    }

    public static Customer createCustomer(String name) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(name.toLowerCase().replace(" ", ".") + "@test.com");
        return customer;
    }
}
