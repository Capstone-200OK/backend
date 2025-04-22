package com.example.demo.dto.sortingHistoryDTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SortingHistoryFileResponseDTO {
    private String previousFolderName;
    private String previousFilePath;
    private String currentFolderName;
    private String currentFilePath;
}
