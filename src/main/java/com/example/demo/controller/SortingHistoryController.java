package com.example.demo.controller;

import com.example.demo.dto.MessageResponse;
import com.example.demo.dto.sortingHistoryDTO.SortingHistoryLatestIdResponseDTO;
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

    /**
     * 자동 분류 기록 저장
     *
     * @param request 자동 분류 요청 정보
     * @return 처리 결과 메시지
     */
    @PostMapping("/save")
    public ResponseEntity<MessageResponse> saveSortingHistory(@RequestBody SortingHistoryRequestDTO request) {
        sortingHistoryService.saveSortingHistory(request);
        return ResponseEntity.ok(new MessageResponse("Sorting history has been saved"));
    }

    /**
     * 자동 분류 복구
     *
     * @param sortingId 복구할 정리 기록 ID
     * @return 처리 결과 메시지
     */
    @PostMapping("/rollback/{sortingId}")
    public ResponseEntity<MessageResponse> rollbackSortingHistory(@PathVariable Long sortingId) {
        sortingHistoryService.rollbackSortingHistory(sortingId);
        return ResponseEntity.ok(new MessageResponse("Sorting history has been rolled back"));
    }

    /**
     * 자동 분류 기록 리스트 조회
     *
     * @param userId 사용자 ID
     * @return 정리 기록 리스트
     */
    @GetMapping("/list/{userId}")
    public ResponseEntity<List<SortingHistoryResponseDTO>> getSortingHistoryFiles(@PathVariable Long userId) {
        List<SortingHistoryResponseDTO> response = sortingHistoryService.getSortingHistoryFiles(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 자동 분류 상세 정보 조회
     *
     * @param sortingId 정리 기록 ID
     * @return 파일 이동 상세 정보
     */
    @GetMapping("/selectedList/{sortingId}")
    public ResponseEntity<SortingHistorySelectedResponseDTO> getSortingHistorySelectedFiles(@PathVariable Long sortingId) {
        SortingHistorySelectedResponseDTO response = sortingHistoryService.getSortingHistorySelectedFiles(sortingId);
        return ResponseEntity.ok(response);
    }

    /**
     * 최근 자동 분류 ID 조회
     *
     * @param userId 사용자 ID
     * @return 최신 정리 기록 ID DTO
     */
    @GetMapping("/latest-id/{userId}")
    public ResponseEntity<SortingHistoryLatestIdResponseDTO> getLatestSortingHistoryId(@PathVariable Long userId) {
        Long latestId = sortingHistoryService.getLatestSortingHistoryId(userId);
        SortingHistoryLatestIdResponseDTO response = new SortingHistoryLatestIdResponseDTO(latestId);
        return ResponseEntity.ok(response);
    }
}
