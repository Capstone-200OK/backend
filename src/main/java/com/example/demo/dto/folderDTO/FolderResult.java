package com.example.demo.dto.folderDTO;

import com.example.demo.entity.Folder;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FolderResult {
    private Folder folder;
    private boolean isNewlyCreated;
}
