package com.example.demo.controller;

import com.example.demo.dto.FileRequestDTO;
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
    public File uploadFile(@RequestBody FileRequestDTO fileRequestDTO) {
        return fileService.uploadFile(fileRequestDTO);
    }
}
