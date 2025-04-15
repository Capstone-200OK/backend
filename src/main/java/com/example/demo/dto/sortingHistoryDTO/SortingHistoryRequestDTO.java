package com.example.demo.dto.sortingHistoryDTO;
import com.example.demo.dto.fileDTO.FileUpdateRequestDTO;
import com.example.demo.dto.folderDTO.FolderUpdateRequestDTO;

import lombok.Data;
import java.util.List;

@Data
public class SortingHistoryRequestDTO {
    private Long userId;
    private List<FileUpdateRequestDTO> fileUpdates;
    private List<FolderUpdateRequestDTO> folderUpdates;
}
