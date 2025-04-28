package com.example.demo.dto.folderDTO;

import com.example.demo.entity.FolderType;
import lombok.Data;

@Data
public class FolderRequestDTO {
    private Long userId;
    private String name;
    private Long parentFolderId;
    private FolderType folderType;
}
