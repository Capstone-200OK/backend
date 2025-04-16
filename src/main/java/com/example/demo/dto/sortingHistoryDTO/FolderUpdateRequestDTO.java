package com.example.demo.dto.sortingHistoryDTO;

import com.example.demo.entity.FolderStatus;
import lombok.Builder;
import lombok.Data;

@Data
public class FolderUpdateRequestDTO {
    private Long folderId;
    private FolderStatus status;

    @Builder
    public FolderUpdateRequestDTO(Long folderId, FolderStatus status) {
        this.folderId = folderId;
        this.status = status;
    }
}
