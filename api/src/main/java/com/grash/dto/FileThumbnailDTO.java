package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FileThumbnailDTO extends FileMiniDTO {
    @Schema(description = "URL of the thumbnail image")
    private String thumbnailUrl;
}
