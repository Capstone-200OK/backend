package com.example.demo.dto.folderDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FolderSearchResponseDTO {
    private Long folderId;
    private String folderName;
    private String parentFolderName;
}