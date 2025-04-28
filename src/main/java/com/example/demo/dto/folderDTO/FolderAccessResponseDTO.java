package com.example.demo.dto.folderDTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FolderAccessResponseDTO {
    private boolean canRead;
    private boolean canWrite;
    private boolean canDelete;
}