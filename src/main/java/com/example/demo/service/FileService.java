package com.example.demo.service;

import com.example.demo.dto.FileRequestDTO;
import com.example.demo.entity.File;
import com.example.demo.entity.Folder;
import com.example.demo.entity.User;
import com.example.demo.repository.FileRepository;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;

    @Transactional
    public File uploadFile(FileRequestDTO fileRequestDTO) {
        User user = userRepository.findById(fileRequestDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Folder folder = folderRepository.findById(fileRequestDTO.getFolderId())
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        File file = File.builder()
                .user(user)
                .folder(folder)
                .name(fileRequestDTO.getName())
                .filePath(fileRequestDTO.getFilePath())
                .fileType(fileRequestDTO.getFileType())
                .size(fileRequestDTO.getSize())
                .build();

        return fileRepository.save(file);
    }
}
