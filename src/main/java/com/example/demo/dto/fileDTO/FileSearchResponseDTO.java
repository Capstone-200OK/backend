package com.example.demo.dto.fileDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileSearchResponseDTO {
    private Long fileId;
    private String fileName;
    private Long parentFolderId;
    private String parentFolderName;
}