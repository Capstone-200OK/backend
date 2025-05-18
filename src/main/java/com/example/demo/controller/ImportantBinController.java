package com.example.demo.controller;

import com.example.demo.dto.MessageResponse;
import com.example.demo.dto.importantBinDTO.ImportantBinFileResponseDTO;
import com.example.demo.dto.importantBinDTO.ImportantBinFolderResponseDTO;
import com.example.demo.dto.importantBinDTO.ImportantBinRequestDTO;
import com.example.demo.service.ImportantBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/important-bin")
@RequiredArgsConstructor
public class ImportantBinController {

    private final ImportantBinService importantBinService;

    /**
     * 중요 문서함에 파일 또는 폴더 추가
     *
     * @param requestDTO 파일 또는 폴더 ID, 사용자 ID 정보 포함
     * @return 처리 결과 메시지
     */
    @PostMapping("/add")
    public ResponseEntity<MessageResponse> addToImportant(@RequestBody ImportantBinRequestDTO requestDTO) {
        importantBinService.addToImportantBin(requestDTO);
        return ResponseEntity.ok(new MessageResponse("Added to important bin"));
    }

    /**
     * 중요 문서함에서 파일 또는 폴더 제거
     *
     * @param importantId 중요 문서함 항목 ID
     * @return 처리 결과 메시지
     */
    @DeleteMapping("/remove/{importantId}")
    public ResponseEntity<MessageResponse> removeFromImportant(@PathVariable Long importantId) {
        importantBinService.removeFromImportantBin(importantId);
        return ResponseEntity.ok(new MessageResponse("Removed from important bin"));
    }

    /**
     * 사용자의 중요 문서함에 포함된 파일 리스트 조회
     *
     * @param userId 사용자 ID
     * @return 파일 리스트
     */
    @GetMapping("/files/{userId}")
    public List<ImportantBinFileResponseDTO> getDeletedFiles(@PathVariable Long userId) {
        return importantBinService.getImportantFiles(userId);
    }

    /**
     * 사용자의 중요 문서함에 포함된 폴더 리스트 조회
     *
     * @param userId 사용자 ID
     * @return 폴더 리스트
     */
    @GetMapping("/folders/{userId}")
    public List<ImportantBinFolderResponseDTO> getDeletedFolders(@PathVariable Long userId) {
        return importantBinService.getImportantFolders(userId);
    }
}
