package com.example.demo.dto.folderDTO;

import lombok.Data;

@Data
public class FolderRequestDTO {
    private Long userId;
    private String name;
    private Long parentFolderId;
}
