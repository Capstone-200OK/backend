package com.example.demo.dto.sortingHistoryDTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SortingHistoryResponseDTO {
    private String sortingId;
    private Long userId;
    private String sortingDate;
}
