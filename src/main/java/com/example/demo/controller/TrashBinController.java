package com.example.demo.controller;

import com.example.demo.dto.MessageResponse;
import com.example.demo.dto.trashBinDTO.TrashBinFileResponseDTO;
import com.example.demo.dto.trashBinDTO.TrashBinFolderResponseDTO;
import com.example.demo.dto.trashBinDTO.TrashBinRequestDTO;
import com.example.demo.service.TrashBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trash")
@RequiredArgsConstructor
public class TrashBinController {

    private final TrashBinService trashBinService;

    // 휴지통으로 파일/폴더 이동
    @PostMapping("/move")
    public ResponseEntity<MessageResponse> moveToTrash(@RequestBody TrashBinRequestDTO dto) {
        trashBinService.moveToTrash(dto);
        return ResponseEntity.ok(new MessageResponse("Moved to trash"));
    }

    // 휴지통에서 파일/폴더 복구
    @PostMapping("/restore")
    public ResponseEntity<MessageResponse> restore(@RequestBody List<Long> trashIds) {
        trashBinService.restoreAll(trashIds);
        return ResponseEntity.ok(new MessageResponse("Restoration completed"));
    }

    // 휴지통에서 파일/폴더 완전 삭제
    @PostMapping("/delete")
    public ResponseEntity<MessageResponse> deletePermanently(@RequestBody List<Long> trashIds) {
        if (trashIds == null || trashIds.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("The list of IDs to delete is empty"));
        }

        trashBinService.deleteAllPermanently(trashIds);
        return ResponseEntity.ok(new MessageResponse("Deleted from trash"));
    }

    // 휴지통에 있는 파일 리스트 반환
    @GetMapping("/files/{userId}")
    public List<TrashBinFileResponseDTO> getDeletedFiles(@PathVariable Long userId) {
        return trashBinService.getDeletedFiles(userId);
    }

    // 휴지통에 있는 폴더 리스트 반환
    @GetMapping("/folders/{userId}")
    public List<TrashBinFolderResponseDTO> getDeletedFolders(@PathVariable Long userId) {
        return trashBinService.getDeletedFolders(userId);
    }
}
