package com.grash.mapper;

import com.grash.dto.MeterMiniDTO;
import com.grash.dto.MeterPatchDTO;
import com.grash.dto.MeterPostDTO;
import com.grash.dto.MeterShowDTO;
import com.grash.model.Meter;
import com.grash.model.Reading;
import com.grash.service.ReadingService;
import com.grash.utils.AuditComparator;
import com.grash.utils.Helper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

@Mapper(componentModel = "spring", uses = {LocationMapper.class, AssetMapper.class, UserMapper.class,
        FileMapper.class, CustomFieldValueMapper.class})
public interface MeterMapper {
    Meter updateMeter(@MappingTarget Meter entity, MeterPatchDTO dto);

    MeterPatchDTO toPatchDto(Meter model);

    @Mapping(target = "image", source = "image", qualifiedByName = "toThumbnailDto")
    MeterShowDTO toShowDto(Meter model, @Context ReadingService readingService);

    @AfterMapping
    default MeterShowDTO toShowDto(Meter model, @MappingTarget MeterShowDTO target,
                                   @Context ReadingService readingService) {
        Optional<Reading> optionalLastReading = readingService.findLastByMeter(target.getId());
        if (optionalLastReading.isPresent()) {
            Reading lastReading = optionalLastReading.get();
            target.setLastReading(lastReading.getCreatedAt());
            Date nextReading = Date.from(
                    Helper.dateToLocalDate(lastReading.getCreatedAt())
                            .plusDays(target.getUpdateFrequency())
                            .atStartOfDay(ZoneId.of(model.getCompany().getCompanySettings().getGeneralPreferences().getTimeZone()))
                            .toInstant()
            );
            target.setNextReading(nextReading);
        }
        return target;
    }

    MeterMiniDTO toMiniDto(Meter model);

    Meter fromPostDto(MeterPostDTO dto);
}
