package com.example.demo.dto.folderDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FolderPathResponseDTO {
    private Long folderId;
    private String folderName;
}