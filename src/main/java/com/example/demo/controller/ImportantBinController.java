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

    // 중요 문서함에 파일/폴더 추가
    @PostMapping("/add")
    public ResponseEntity<MessageResponse> addToImportant(@RequestBody ImportantBinRequestDTO requestDTO) {
        importantBinService.addToImportantBin(requestDTO);
        return ResponseEntity.ok(new MessageResponse("Added to important bin"));
    }

    // 중요 문서함에서 파일/폴더 제거
    @DeleteMapping("/remove/{importantId}")
    public ResponseEntity<MessageResponse> removeFromImportant(@PathVariable Long importantId) {
        importantBinService.removeFromImportantBin(importantId);
        return ResponseEntity.ok(new MessageResponse("Removed from important bin"));
    }

    // 중요 문서함에 있는 파일 리스트 반환
    @GetMapping("/files/{userId}")
    public List<ImportantBinFileResponseDTO> getDeletedFiles(@PathVariable Long userId) {
        return importantBinService.getImportantFiles(userId);
    }

    // 중요 문서함에 있는 폴더 리스트 반환
    @GetMapping("/folders/{userId}")
    public List<ImportantBinFolderResponseDTO> getDeletedFolders(@PathVariable Long userId) {
        return importantBinService.getImportantFolders(userId);
    }
}
