package com.example.demo.dto.fileDTO;

import lombok.Data;

@Data
public class RenameRequestDTO {
    private Long fileId;
    private String newName;
    private String newFilePath;
}
