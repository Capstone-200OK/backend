package com.example.demo.controller;

import com.example.demo.dto.fileDTO.FileRequestDTO;
import com.example.demo.dto.fileDTO.MoveRequestDTO;
import com.example.demo.dto.fileDTO.RenameRequestDTO;
import com.example.demo.entity.File;
import com.example.demo.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;
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