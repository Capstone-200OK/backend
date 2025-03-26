package com.example.demo.controller;

import com.example.demo.dto.FolderRequestDTO;
import com.example.demo.entity.Folder;
import com.example.demo.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/folder")
@RequiredArgsConstructor
public class FolderController {
    private final FolderService folderService;
    @PostMapping("/add")
    public Folder addFolder(@RequestBody FolderRequestDTO folderRequestDTO) {
        return folderService.addFolder(folderRequestDTO);
    }
}
