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

    /**
     * 파일 또는 폴더를 휴지통으로 이동
     *
     * @param dto 휴지통 이동 요청 정보
     * @return 처리 결과 메시지
     */
    @PostMapping("/move")
    public ResponseEntity<MessageResponse> moveToTrash(@RequestBody TrashBinRequestDTO dto) {
        trashBinService.moveToTrash(dto);
        return ResponseEntity.ok(new MessageResponse("Moved to trash"));
    }

    /**
     * 휴지통에서 파일 및 폴더 복구
     *
     * @param trashIds 복구할 항목 ID 리스트
     * @return 처리 결과 메시지
     */
    @PostMapping("/restore")
    public ResponseEntity<MessageResponse> restore(@RequestBody List<Long> trashIds) {
        trashBinService.restoreAll(trashIds);
        return ResponseEntity.ok(new MessageResponse("Restoration completed"));
    }

    /**
     * 휴지통에서 파일 및 폴더 완전 삭제
     *
     * @param trashIds 삭제할 항목 ID 리스트
     * @return 처리 결과 메시지
     */
    @PostMapping("/delete")
    public ResponseEntity<MessageResponse> deletePermanently(@RequestBody List<Long> trashIds) {
        if (trashIds == null || trashIds.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("The list of IDs to delete is empty"));
        }

        trashBinService.deleteAllPermanently(trashIds);
        return ResponseEntity.ok(new MessageResponse("Deleted from trash"));
    }

    /**
     * 사용자 휴지통에 있는 파일 리스트 조회
     *
     * @param userId 사용자 ID
     * @return 파일 응답 DTO 리스트
     */
    @GetMapping("/files/{userId}")
    public List<TrashBinFileResponseDTO> getDeletedFiles(@PathVariable Long userId) {
        return trashBinService.getDeletedFiles(userId);
    }

    /**
     * 사용자 휴지통에 있는 폴더 리스트 조회
     *
     * @param userId 사용자 ID
     * @return 폴더 응답 DTO 리스트
     */
    @GetMapping("/folders/{userId}")
    public List<TrashBinFolderResponseDTO> getDeletedFolders(@PathVariable Long userId) {
        return trashBinService.getDeletedFolders(userId);
    }
}
