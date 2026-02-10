package com.grash.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailAttachmentDTO {
    private String attachmentName;
    private byte[] attachmentData;
    private String attachmentType;
}
