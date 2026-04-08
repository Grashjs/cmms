package com.grash.mapper;

import com.grash.dto.comment.CommentPatchDTO;
import com.grash.dto.comment.CommentPostDTO;
import com.grash.dto.comment.CommentShowDTO;
import com.grash.model.Comment;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    Comment updateComment(@MappingTarget Comment entity,
                                                        CommentPatchDTO dto);

    Comment fromPostDto(@Valid CommentPostDTO dto);

    CommentShowDTO toShowDto(@Valid Comment model);
}
