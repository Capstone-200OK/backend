package com.example.demo.dto.trashBinDTO;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class TrashBinFolderResponseDTO {
    private Long trashId;
    private Long folderId;
    private String folderName;
    private Timestamp deletedAt;
}