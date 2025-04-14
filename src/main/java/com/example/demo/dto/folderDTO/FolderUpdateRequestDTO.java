package com.example.demo.dto.folderDTO;

import com.example.demo.entity.FolderStatus;
import lombok.Data;

@Data
public class FolderUpdateRequestDTO {
    private Long folderId;
    private FolderStatus status;
}
