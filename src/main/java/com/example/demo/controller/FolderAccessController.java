package com.example.demo.controller;

import com.example.demo.dto.folderDTO.FolderAccessRequestDTO;
import com.example.demo.dto.folderDTO.FolderAccessResponseDTO;
import com.example.demo.entity.Folder;
import com.example.demo.entity.User;
import com.example.demo.service.FolderAccessService;
import com.example.demo.service.FolderService;
import com.example.demo.service.UserService;
import com.example.demo.util.FolderPermissionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/folder-access")
@RequiredArgsConstructor
public class FolderAccessController {

    private final FolderAccessService folderAccessService;
    private final UserService userService;
    private final FolderService folderService;

    /**
     * 폴더에 대한 접근 권한 부여
     *
     * @param request 권한 요청 DTO (userId, folderId, 권한 정보 포함)
     * @return 권한 부여 결과 문자열
     */
    @PostMapping("/grant")
    public ResponseEntity<String> grantAccess(@RequestBody FolderAccessRequestDTO request) {
        folderAccessService.grantAccess(request.getUserId(), request.getFolderId(), request.getChmod());

        return ResponseEntity.ok("Access granted with permission: " + FolderPermissionUtil.toString(request.getChmod()));
    }

    /**
     * 폴더 접근 권한 확인 (읽기, 쓰기, 삭제)
     *
     * @param userId 사용자 ID
     * @param folderId 폴더 ID
     * @return 권한 상태 DTO
     */
    @GetMapping("/check")
    public ResponseEntity<FolderAccessResponseDTO> checkAccess(@RequestParam Long userId, @RequestParam Long folderId) {
        User user = userService.getUserById(userId);
        Folder folder = folderService.getFolderById(folderId);

        boolean canRead = folderAccessService.canRead(user, folder);
        boolean canWrite = folderAccessService.canWrite(user, folder);
        boolean canDelete = folderAccessService.canDelete(user, folder);

        FolderAccessResponseDTO response = new FolderAccessResponseDTO(canRead, canWrite, canDelete);
        return ResponseEntity.ok(response);
    }
}
