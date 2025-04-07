package com.example.demo.controller;

import com.example.demo.dto.FolderRequestDTO;
import com.example.demo.entity.Folder;
import com.example.demo.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
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
    public Map<String, Object> findFolderName(@RequestBody FolderRequestDTO folderRequestDTO) {
        Optional<Folder> found = folderService.findFolderByName(folderRequestDTO);

        Map<String, Object> result = new HashMap<>();
        if (found.isPresent()) {
            // 폴더가 있
            Folder folder = found.get();
            result.put("found", true);
            result.put("folderId", folder.getId());
        } else {
            // 폴더가 없음
            result.put("found", false);
            result.put("folderId", null);
        }
        return result; // JSON으로 { "found":true, "folderId":1 } or { "found":false, "folderId":null }
    }

    @PostMapping("/findOrCreate")
    public Folder findOrCreateFolder(@RequestBody FolderRequestDTO folderRequestDTO) {
        return folderService.findOrCreateFolder(folderRequestDTO);
    }

    public record FolderDTO(Long id, String name, Long parentId, Long userId) {
        public static FolderDTO fromEntity(Folder folder) {
            return new FolderDTO(
                    folder.getId(),
                    folder.getName(),
                    folder.getParentFolder() == null ? null : folder.getParentFolder().getId(),
                    folder.getUser().getId()
            );
        }
    }
}