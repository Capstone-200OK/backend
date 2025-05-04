package com.example.demo.dto.fileDTO;

import lombok.Data;

@Data
public class RenameRequestDTO {
    private Long fileId;
    private String newName;

    public RenameRequestDTO(Long fileId, String newName) {
        this.fileId = fileId;
        this.newName = newName;
    }
//    private String newFilePath;
}
