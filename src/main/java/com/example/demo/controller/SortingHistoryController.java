package com.example.demo.controller;

import com.example.demo.dto.sortingHistoryDTO.SortingHistoryRequestDTO;
import com.example.demo.service.SortingHistoryService;
import com.example.demo.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sorting-history")
public class SortingHistoryController {

    private final SortingHistoryService sortingHistoryService;

    // 자동 분류 기록 저장
    @PostMapping("/save")
    public ResponseEntity<MessageResponse> saveSortingHistory(@RequestBody SortingHistoryRequestDTO request) {
        sortingHistoryService.saveSortingHistory(request);
        return ResponseEntity.ok(new MessageResponse("자동 분류 기록이 저장되었습니다."));
    }

    // 자동 분류 복구
    @PostMapping("/rollback/{sortingId}")
    public ResponseEntity<MessageResponse> rollbackSortingHistory(@PathVariable Long sortingId) {
        sortingHistoryService.rollbackSortingHistory(sortingId);
        return ResponseEntity.ok(new MessageResponse("자동 분류가 복구되었습니다."));
    }
}
