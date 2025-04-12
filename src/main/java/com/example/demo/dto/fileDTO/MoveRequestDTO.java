package com.example.demo.dto.fileDTO;

import lombok.Data;

@Data
public class MoveRequestDTO {
    private Long fileId;
    private String filePath;
    private Long folderId;

    public MoveRequestDTO(Long fileId, Long folderId, String source) {
        this.fileId = fileId;
        this.filePath = source;
        this.folderId = folderId;
    }
}
