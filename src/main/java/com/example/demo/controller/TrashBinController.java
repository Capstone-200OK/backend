package com.example.demo.controller;

import com.example.demo.dto.trashBinDTO.TrashBinRequestDTO;
import com.example.demo.dto.trashBinDTO.TrashBinResponseDTO;
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
    @DeleteMapping("/delete")
    public ResponseEntity<MessageResponse> deletePermanently(@RequestBody List<Long> trashIds) {
        trashBinService.deleteAllPermanently(trashIds);
        return ResponseEntity.ok(new MessageResponse("휴지통에서 삭제되었습니다."));
    }

    // 휴지통 목록 확인
    @GetMapping("/list/{userId}")
    public List<TrashBinResponseDTO> getTrashFiles(@PathVariable Long userId) {
        return trashBinService.getTrashFilesByUser(userId);
    }

    // 자동 삭제 테스트용
    @PostMapping("/autoDelete")
    public ResponseEntity<Void> deleteExpiredTrash() {
        trashBinService.deleteExpiredTrash();
        return ResponseEntity.ok().build();
    }
}
