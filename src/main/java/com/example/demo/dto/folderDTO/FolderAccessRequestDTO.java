package com.example.demo.dto.folderDTO;

import lombok.Data;

@Data
public class FolderAccessRequestDTO {
    private Long userId;
    private Long folderId;
    private int chmod;  // 예: 7 = r+w+d
}
