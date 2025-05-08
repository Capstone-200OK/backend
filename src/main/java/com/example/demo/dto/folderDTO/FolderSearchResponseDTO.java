package com.example.demo.dto.folderDTO;

import com.example.demo.entity.FolderType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FolderSearchResponseDTO {
    private Long folderId;
    private String folderName;
    private String parentFolderName;
    private FolderType folderType;
}