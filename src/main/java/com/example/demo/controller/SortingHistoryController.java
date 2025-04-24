package com.example.demo.controller;

import com.example.demo.dto.MessageResponse;
import com.example.demo.dto.sortingHistoryDTO.SortingHistoryRequestDTO;
import com.example.demo.dto.sortingHistoryDTO.SortingHistoryResponseDTO;
import com.example.demo.dto.sortingHistoryDTO.SortingHistorySelectedResponseDTO;
import com.example.demo.service.SortingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/list/{userId}")
    public ResponseEntity<List<SortingHistoryResponseDTO>> getSortingHistoryFiles(@PathVariable Long userId) {
        List<SortingHistoryResponseDTO> response = sortingHistoryService.getSortingHistoryFiles(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/selectedList/{sortingId}")
    public ResponseEntity<SortingHistorySelectedResponseDTO> getSortingHistorySelectedFiles(@PathVariable Long sortingId) {
        SortingHistorySelectedResponseDTO response = sortingHistoryService.getSortingHistorySelectedFiles(sortingId);
        return ResponseEntity.ok(response);
    }
}
