package com.grash.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.savedView.SavedViewPatchDTO;
import com.grash.dto.savedView.SavedViewPostDTO;
import com.grash.dto.savedView.SavedViewShowDTO;
import com.grash.exception.CustomException;
import com.grash.model.SavedView;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Maps between the entity, which stores criteria and layout as JSON text, and the DTOs,
 * which expose them as real objects. The conversion methods below are picked up by MapStruct
 * automatically because the source and target types differ.
 */
@Mapper(componentModel = "spring", uses = {UserMapper.class})
public abstract class SavedViewMapper {

    @Autowired
    private ObjectMapper objectMapper;

    @Mapping(target = "editable", ignore = true)
    public abstract SavedViewShowDTO toShowDto(SavedView model);

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "company", ignore = true)
    public abstract SavedView fromPostDto(SavedViewPostDTO dto);

    /**
     * Unlike the other update mappers in this package, null means "leave unchanged" here.
     * Renaming a view is the frequent case and the client has no reason to resend the whole
     * criteria and layout for it; with MapStruct's default strategy that rename would wipe
     * both.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "entityType", ignore = true)
    public abstract SavedView updateSavedView(@MappingTarget SavedView entity, SavedViewPatchDTO dto);

    protected SearchCriteria toCriteria(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, SearchCriteria.class);
        } catch (JsonProcessingException e) {
            // Only reachable if the column was edited outside the application. Failing loudly
            // beats handing the list page a half-parsed filter set.
            throw new CustomException("Saved view holds unreadable criteria", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    protected JsonNode toLayout(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new CustomException("Saved view holds unreadable column layout",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    protected String fromCriteria(SearchCriteria criteria) {
        if (criteria == null) return null;
        try {
            return objectMapper.writeValueAsString(criteria);
        } catch (JsonProcessingException e) {
            throw new CustomException("Criteria cannot be serialised", HttpStatus.NOT_ACCEPTABLE);
        }
    }

    protected String fromLayout(JsonNode layout) {
        return layout == null ? null : layout.toString();
    }
}
