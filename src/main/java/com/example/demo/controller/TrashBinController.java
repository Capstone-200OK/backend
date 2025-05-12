package com.example.demo.controller;

import com.example.demo.dto.trashBinDTO.*;
import com.example.demo.dto.MessageResponse;
import com.example.demo.service.TrashBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/trash")
@RequiredArgsConstructor
public class TrashBinController {

    private final TrashBinService trashBinService;

    // 휴지통으로 이동
    @PostMapping("/move")
    public ResponseEntity<MessageResponse> moveToTrash(@RequestBody TrashBinRequestDTO dto) {
        trashBinService.moveToTrash(dto);
        return ResponseEntity.ok(new MessageResponse("휴지통으로 이동되었습니다."));
    }

    // 휴지통에서 복구
    @PostMapping("/restore")
    public ResponseEntity<MessageResponse> restore(@RequestBody List<Long> trashIds) {
        trashBinService.restoreAll(trashIds);
        return ResponseEntity.ok(new MessageResponse("복구가 완료되었습니다."));
    }

    // 휴지통에서 완전 삭제
    @PostMapping("/delete")
    public ResponseEntity<MessageResponse> deletePermanently(@RequestBody List<Long> trashIds) {
        if (trashIds == null || trashIds.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("삭제할 ID 목록이 비어있습니다."));
        }

        trashBinService.deleteAllPermanently(trashIds);
        return ResponseEntity.ok(new MessageResponse("휴지통에서 삭제되었습니다."));
    }

    // 휴지통 목록 확인

    @GetMapping("/files/{userId}")
    public List<TrashBinFileResponseDTO> getDeletedFiles(@PathVariable Long userId) {
        return trashBinService.getDeletedFiles(userId);
    }

    @GetMapping("/folders/{userId}")
    public List<TrashBinFolderResponseDTO> getDeletedFolders(@PathVariable Long userId) {
        return trashBinService.getDeletedFolders(userId);
    }

    // 자동 삭제 테스트용
    @PostMapping("/autoDelete")
    public ResponseEntity<Void> deleteExpiredTrash() {
        trashBinService.deleteExpiredTrash();
        return ResponseEntity.ok().build();
    }
}
