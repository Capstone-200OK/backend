package com.example.demo.service;

import com.example.demo.dto.folderDTO.FolderRequestDTO;
import com.example.demo.entity.Folder;
import com.example.demo.entity.User;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;

    @Transactional
    public Optional<Folder> findFolderByName(FolderRequestDTO folderRequestDTO) {
        User user = userRepository.findById(folderRequestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println(user);
        Folder parent = null;
        if (folderRequestDTO.getParentFolderId() != null) {
            parent = folderRepository.findById(folderRequestDTO.getParentFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 폴더를 찾을 수 없습니다."));
        }
        System.out.println(parent);
        if (parent == null) {
            Folder folder =  folderRepository.findByNameAndParentFolderIsNullAndUser(folderRequestDTO.getName(), user).get();
            System.out.println(folder);
            return folderRepository.findByNameAndParentFolderIsNullAndUser(folderRequestDTO.getName(), user);
        } else {
            return folderRepository.findByNameAndParentFolderAndUser(folderRequestDTO.getName(), parent, user);
        }
    }

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

        return folderRepository.save(folder);
    }

    @Transactional
    public Folder findOrCreateFolder(FolderRequestDTO folderRequestDTO) {
        // 존재하는지 확인
        Optional<Folder> found = findFolderByName(folderRequestDTO);
        // 있으면 그 폴더 반환
        if (found.isPresent()) {
            return found.get();
        }
        // 없으면 새로 생성
        return addFolder(folderRequestDTO);
    }
}