package com.example.demo.dto.sortingHistoryDTO;

import lombok.Builder;
import lombok.Data;

@Data
public class FileUpdateRequestDTO {
    private Long fileId;
    private String previousFilePath;
    private Long previousFolderId;

    @Builder
    public FileUpdateRequestDTO(Long fileId, String previousFilePath, Long previousFolderId) {
        this.fileId = fileId;
        this.previousFilePath = previousFilePath;
        this.previousFolderId = previousFolderId;
    }
}
