package com.example.demo.service;

import com.example.demo.dto.FolderRequestDTO;
import com.example.demo.entity.Folder;
import com.example.demo.entity.User;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FolderService {
    final private UserRepository userRepository;
    final private FolderRepository folderRepository;

    @Transactional
    public Folder addFolder(FolderRequestDTO folderRequestDTO) {
        User user = userRepository.findById(folderRequestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Folder parent = null;
        if (folderRequestDTO.getParentFolderId() != null) {
            parent = folderRepository.findById(folderRequestDTO.getParentFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 폴더를 찾을 수 없습니다."));
        }

        Folder folder = Folder.builder()
                .name(folderRequestDTO.getName())
                .user(user)
                .parentFolder(parent)
                .build();
        System.out.println(folder);
        return folderRepository.save(folder);
    }
}
