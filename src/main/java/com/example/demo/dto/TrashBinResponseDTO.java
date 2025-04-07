package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class TrashBinResponseDTO {
    private Long trashId;

    private Long fileId;
    private String fileName;
    private String fileType;
    private Long size;

    private Long folderId;
    private String folderName;

    private Timestamp deletedAt;
}
