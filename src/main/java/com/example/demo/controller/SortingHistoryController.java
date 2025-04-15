package com.example.demo.controller;

import com.example.demo.dto.sortingHistoryDTO.SortingHistoryRequestDTO;
import com.example.demo.service.SortingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sorting-history")
public class SortingHistoryController {

    private final SortingHistoryService sortingHistoryService;

    // 정리 기록 저장
    @PostMapping("/save")
    public void saveSortingHistory(@RequestBody SortingHistoryRequestDTO request) {
        sortingHistoryService.saveSortingHistory(request);
    }

    // 정리 기록 되돌리기
    @PostMapping("/rollback/{sortingId}")
    public void rollbackSortingHistory(@PathVariable Long sortingId) {
        sortingHistoryService.rollbackSortingHistory(sortingId);
    }
}
