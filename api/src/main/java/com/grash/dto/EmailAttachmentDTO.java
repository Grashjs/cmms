package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO for email attachment data")
public class EmailAttachmentDTO {
    @Schema(description = "Attachment file name")
    private String attachmentName;
    
    @Schema(description = "Attachment file content as byte array")
    private byte[] attachmentData;
    
    @Schema(description = "Attachment MIME type")
    private String attachmentType;
}
