package com.example.demo.controller;

import com.example.demo.dto.folderDTO.*;
import com.example.demo.entity.Folder;
import com.example.demo.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/folder")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    /**
     * 폴더 생성 요청
     *
     * @param request 폴더 생성 정보
     * @return 생성된 폴더 DTO
     */
    @PostMapping("/add")
    public FolderDTO addFolder(@RequestBody FolderRequestDTO request) {
        Folder created = folderService.addFolder(request);
        return FolderDTO.fromEntity(created);
    }

    /**
     * 이름으로 폴더 존재 여부 확인
     *
     * @param request 폴더 이름, 부모 ID 등 정보
     * @return found: true/false, folderId: 존재 시 ID
     */
    @PostMapping("/find")
    public Map<String, Object> findFolderName(@RequestBody FolderRequestDTO request) {
        Optional<Folder> found = folderService.findFolderByName(request);

        Map<String, Object> result = new HashMap<>();
        result.put("found", found.isPresent());
        result.put("folderId", found.map(Folder::getId).orElse(null));
        return result;
    }

    /**
     * 폴더를 찾고 없으면 새로 생성
     *
     * @param request 폴더 요청 정보
     * @return 찾거나 새로 생성된 폴더
     */
    @PostMapping("/findOrCreate")
    public Folder findOrCreateFolder(@RequestBody FolderRequestDTO request) {
        return folderService.findOrCreateFolder(request);
    }

    /**
     * 사용자와 경로 기반으로 폴더 ID 조회
     *
     * @param userId 사용자 ID
     * @param path 경로 문자열
     * @return folderId 반환
     */
    @GetMapping("/id-by-path")
    public ResponseEntity<Map<String, Long>> getFolderIdByPath(
            @RequestParam Long userId,
            @RequestParam String path
    ) {
        Long folderId = folderService.getFolderIdByPath(userId, path);
        Map<String, Long> result = new HashMap<>();
        result.put("folderId", folderId);
        return ResponseEntity.ok(result);
    }

    /**
     * 사용자가 접근 가능한 외부 클라우드 루트 폴더 리스트 조회
     *
     * @param userId 사용자 ID
     * @return 폴더 리스트 (Python 연동용 DTO)
     */
    @GetMapping("/cloud-visible/{userId}")
    public ResponseEntity<List<FolderPythonRequestDTO>> getCloudVisibleFolders(@PathVariable Long userId) {
        return ResponseEntity.ok(folderService.getAccessibleCloudRoots(userId));
    }

    /**
     * 폴더 계층 구조 조회 (하위 폴더 포함, Python용 구조)
     *
     * @param folderId 시작 폴더 ID
     * @param userId 사용자 ID
     * @return 폴더 계층 구조 DTO
     */
    @GetMapping("/hierarchy/{folderId}/{userId}")
    public FolderPythonRequestDTO getFolderHierarchy(
            @PathVariable Long folderId,
            @PathVariable Long userId
    ) {
        return folderService.getFolderHierarchy(folderId, userId);
    }

    /**
     * 선택지로 보여줄 수 있는 폴더 리스트 조회
     *
     * @param userId 사용자 ID
     * @return 선택 가능한 폴더 리스트
     */
    @GetMapping("/selectable/{userId}")
    public List<FolderSelectableDTO> getSelectableFolders(@PathVariable Long userId) {
        return folderService.findSelectableFolders(userId);
    }

    /**
     * 폴더 경로 조회 (userId 없이 단일 폴더 기준)
     *
     * @param folderId 폴더 ID
     * @return 경로 정보 리스트
     */
    @GetMapping("/path/{folderId}")
    public ResponseEntity<List<FolderPathResponseDTO>> getFolderPathAPI(@PathVariable Long folderId) {
        List<FolderPathResponseDTO> path = folderService.getFolderPath(folderId, null);
        return ResponseEntity.ok(path);
    }

    /**
     * 클라우드 폴더 경로 조회 (userId 포함)
     *
     * @param userId 사용자 ID
     * @param folderId 폴더 ID
     * @return 경로 정보 리스트
     */
    @GetMapping("/cloudPath/{userId}/{folderId}")
    public ResponseEntity<List<FolderPathResponseDTO>> getCloudFolderPathAPI(@PathVariable Long userId, @PathVariable Long folderId) {
        List<FolderPathResponseDTO> path = folderService.getFolderPath(folderId, userId);
        return ResponseEntity.ok(path);
    }

    /**
     * 사용자 기준으로 폴더 검색
     *
     * @param userId 사용자 ID
     * @param input 검색 키워드
     * @return 검색된 폴더 리스트
     */
    @GetMapping("/search/{userId}/{input}")
    public ResponseEntity<List<FolderSearchResponseDTO>> searchFolder(@PathVariable Long userId, @PathVariable String input) {
        List<FolderSearchResponseDTO> folders = folderService.searchFolder(userId, input);
        return ResponseEntity.ok(folders);
    }

    /**
     * 클라이언트에 반환할 폴더 요약 DTO
     */
    public record FolderDTO(Long id, String name, Long parentId, Long userId) {
        public static FolderDTO fromEntity(Folder folder) {
            return new FolderDTO(
                    folder.getId(),
                    folder.getName(),
                    folder.getParentFolder() != null ? folder.getParentFolder().getId() : null,
                    folder.getUser().getId()
            );
        }
    }
}
