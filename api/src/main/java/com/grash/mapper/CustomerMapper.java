package com.grash.mapper;

import com.grash.dto.CustomerMiniDTO;
import com.grash.dto.CustomerPatchDTO;
import com.grash.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    Customer updateCustomer(@MappingTarget Customer entity, CustomerPatchDTO dto);

    @Mappings({})
    CustomerPatchDTO toPatchDto(Customer model);

    CustomerMiniDTO toMiniDto(Customer model);
}
