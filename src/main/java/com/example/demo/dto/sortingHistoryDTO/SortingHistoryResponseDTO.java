package com.example.demo.dto.sortingHistoryDTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 자동 분류 기록 응답 DTO
 * 파일 정리 기록 목록을 반환
 */
@Data
@AllArgsConstructor
public class SortingHistoryResponseDTO {
    private List<SortingHistoryFileResponseDTO> fileHistories;
}
