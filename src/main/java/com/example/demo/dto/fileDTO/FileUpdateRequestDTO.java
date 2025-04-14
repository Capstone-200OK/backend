package com.example.demo.dto.fileDTO;

import lombok.Data;

@Data
public class FileUpdateRequestDTO {
    private Long fileId;
    private String newName;
    private String newFilePath;
    private Long newFolderId;
}
