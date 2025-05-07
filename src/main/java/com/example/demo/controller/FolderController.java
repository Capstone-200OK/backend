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

    @PostMapping("/add")
    public FolderDTO addFolder(@RequestBody FolderRequestDTO request) {
        Folder created = folderService.addFolder(request);
        return FolderDTO.fromEntity(created);
    }

    @PostMapping("/find")
    public Map<String, Object> findFolderName(@RequestBody FolderRequestDTO request) {
        Optional<Folder> found = folderService.findFolderByName(request);

        Map<String, Object> result = new HashMap<>();
        result.put("found", found.isPresent());
        result.put("folderId", found.map(Folder::getId).orElse(null));
        return result;
    }

    @PostMapping("/findOrCreate")
    public Folder findOrCreateFolder(@RequestBody FolderRequestDTO request) {
        return folderService.findOrCreateFolder(request);
    }

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

    @GetMapping("/hierarchy/{folderId}/{userId}")
    public FolderPythonRequestDTO getFolderHierarchy(
            @PathVariable Long folderId,
            @PathVariable Long userId
    ) {
        return folderService.getFolderHierarchy(folderId, userId);
    }

    @GetMapping("/selectable/{userId}")
    public List<FolderSelectableDTO> getSelectableFolders(@PathVariable Long userId) {
        return folderService.findSelectableFolders(userId);
    }

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

    @GetMapping("/path/{folderId}")
    public ResponseEntity<List<FolderPathResponseDTO>> getFolderPathAPI(@PathVariable Long folderId) {
        List<FolderPathResponseDTO> path = folderService.getFolderPath(folderId);
        return ResponseEntity.ok(path);
    }

    @GetMapping("/search/{userId}/{input}")
    public ResponseEntity<List<FolderSearchResponseDTO>> searchFolder(@PathVariable Long userId, @PathVariable String input) {
        List<FolderSearchResponseDTO> folders = folderService.searchFolder(userId, input);
        return ResponseEntity.ok(folders);
    }
}
