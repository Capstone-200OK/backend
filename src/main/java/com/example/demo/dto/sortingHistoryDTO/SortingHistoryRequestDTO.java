package com.example.demo.dto.sortingHistoryDTO;

import com.example.demo.dto.fileDTO.FileUpdateRequestDTO;
import com.example.demo.dto.folderDTO.FolderUpdateRequestDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class SortingHistoryRequestDTO {
    private Long userId;
    private List<FileUpdateRequestDTO> fileUpdates;
    private List<FolderUpdateRequestDTO> folderUpdates;

    @Builder
    public SortingHistoryRequestDTO(Long userId, List<FileUpdateRequestDTO> fileUpdates, List<FolderUpdateRequestDTO> folderUpdates) {
        this.userId = userId;
        this.fileUpdates = fileUpdates;
        this.folderUpdates = folderUpdates;
    }
}
