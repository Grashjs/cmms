package com.grash.mapper;

import com.grash.dto.SchedulePatchDTO;
import com.grash.model.Schedule;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {
    /**
     * Absent fields leave the stored value alone, so a caller wanting to pause a
     * schedule can send only that flag instead of echoing back a whole schedule
     * it would otherwise have to fetch first.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Schedule updateSchedule(@MappingTarget Schedule entity, SchedulePatchDTO dto);

    @Mappings({})
    SchedulePatchDTO toPatchDto(Schedule model);
}
