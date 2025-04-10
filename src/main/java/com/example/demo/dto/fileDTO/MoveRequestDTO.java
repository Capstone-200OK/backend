package com.example.demo.dto.fileDTO;

import lombok.Data;

@Data
public class MoveRequestDTO {
    private Long fileId;
    private String filePath;
    private Long folderId;
}
