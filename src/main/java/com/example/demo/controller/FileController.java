package com.example.demo.controller;

import com.example.demo.dto.fileDTO.FileRequestDTO;
import com.example.demo.dto.fileDTO.MoveRequestDTO;
import com.example.demo.dto.fileDTO.RenameRequestDTO;
import com.example.demo.entity.File;
import com.example.demo.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;
    @GetMapping("/fileCount")
    public Long fileCount(@RequestParam Long id){
        return fileService.countByFolderId(id);
    }
    @PostMapping("/upload")
    public FileDTO uploadFile(@RequestBody FileRequestDTO fileRequestDTO) {
        File file = fileService.uploadFile(fileRequestDTO);
        return FileDTO.fromEntity(file);
    }

    @PostMapping("/rename")
    public void renameFile(@RequestBody RenameRequestDTO renameRequestDTO) {
        fileService.renameFile(renameRequestDTO);
    }

    @PostMapping("/move")
    public void moveFile(@RequestBody MoveRequestDTO moveRequestDTO) {
        fileService.moveFile(moveRequestDTO);
    }

/*    @PostMapping("/delete")
    public void deleteFile(@RequestBody DeleteRequestDTO deleteRequestDTO) {
        fileService.deleteFile(deleteRequestDTO);
    }*/

    @GetMapping("/list")
    public ResponseEntity<List<FileDTO>> getFilesByFolder(@RequestParam Long folderId) {
        List<File> files = fileService.getFilesByFolder(folderId);
        List<FileDTO> response = files.stream()
                .map(FileDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }


    public record FileDTO(Long id, String name, Long folderId) {
        public static FileDTO fromEntity(File file) {
            return new FileDTO(
                    file.getId(),
                    file.getName(),
                    file.getFolder() == null ? null : file.getFolder().getId()
            );
        }
    }
}