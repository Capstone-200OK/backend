package com.example.demo.controller;

import com.example.demo.dto.MessageResponse;
import com.example.demo.dto.importantBinDTO.*;
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

    @PostMapping("/add")
    public ResponseEntity<MessageResponse> addToImportant(@RequestBody ImportantBinRequestDTO requestDTO) {
        importantBinService.addToImportantBin(requestDTO);
        return ResponseEntity.ok(new MessageResponse("중요 문서함에 추가되었습니다."));
    }

    @DeleteMapping("/remove/{importantId}")
    public ResponseEntity<MessageResponse> removeFromImportant(@PathVariable Long importantId) {
        importantBinService.removeFromImportantBin(importantId);
        return ResponseEntity.ok(new MessageResponse("중요 문서함에서 제거되었습니다."));
    }

    @GetMapping("/list/{userId}")
    public List<ImportantBinResponseDTO> getTrashFiles(@PathVariable Long userId) {
        return importantBinService.getImportantListByUser(userId);
    }
}
