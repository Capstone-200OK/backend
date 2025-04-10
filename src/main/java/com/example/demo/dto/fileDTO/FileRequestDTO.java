package com.example.demo.dto.fileDTO;

import lombok.Data;

@Data
public class FileRequestDTO {
    private String name;
    private Long userId;
    private Long folderId;
    private String fileType;
    private String filePath;
    private Long size;
}
