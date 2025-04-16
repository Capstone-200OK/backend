package com.example.demo.dto.fileDTO;

import lombok.Builder;
import lombok.Data;

@Data
public class FileUpdateRequestDTO {
    private Long fileId;
    private String newName;
    private String newFilePath;
    private Long newFolderId;

    @Builder
    public FileUpdateRequestDTO(Long fileId, String newName, String newFilePath, Long newFolderId) {
        this.fileId = fileId;
        this.newName = newName;
        this.newFilePath = newFilePath;
        this.newFolderId = newFolderId;
    }
}
