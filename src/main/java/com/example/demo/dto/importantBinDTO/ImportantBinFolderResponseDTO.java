package com.example.demo.dto.importantBinDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportantBinFolderResponseDTO {
    private Long importantId;
    private Long folderId;
    private String folderName;
}
