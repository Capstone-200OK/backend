package com.example.demo.service;

import com.example.demo.dto.fileDTO.FilePythonRequestDTO;
import com.example.demo.dto.folderDTO.FolderPythonRequestDTO;
import com.example.demo.dto.folderDTO.FolderRequestDTO;
import com.example.demo.entity.File;
import com.example.demo.entity.Folder;
import com.example.demo.entity.User;
import com.example.demo.repository.FileRepository;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;

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
                .isDeleted(false)
                .build();

        return folderRepository.save(folder);
    }

    @Transactional
    public Folder findOrCreateFolder(FolderRequestDTO folderRequestDTO) {
        // 존재하는지 확인
        System.out.println(folderRequestDTO);
        Optional<Folder> found = findFolderByName(folderRequestDTO);
        // 있으면 그 폴더 반환
        return found.orElseGet(() -> addFolder(folderRequestDTO));
        // 없으면 새로 생성
    }

    @Transactional(readOnly = true)
    public FolderPythonRequestDTO getFolderHierarchy(Long folderId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        return buildFolderDTO(folder);
    }

    private FolderPythonRequestDTO buildFolderDTO(Folder folder) {
        // 1) 기본 DTO 생성
        FolderPythonRequestDTO dto = new FolderPythonRequestDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());
        dto.setIsDeleted(folder.getIsDeleted());

        // 2) 파일 목록 조회 -> FileDTO 변환
        //    (다른 방법: folder.getFiles() OneToMany가 있다면 바로 사용 가능)
        List<File> files = fileRepository.findByFolderId(folder.getId());
        List<FilePythonRequestDTO> fileDTOList = files.stream()
                .map(f -> new FilePythonRequestDTO(
                        f.getId(),
                        f.getName(),
                        f.getFilePath(),
                        f.getFileType(),
                        f.getSize(),
                        f.getIsDeleted()
                ))
                .toList();
        dto.setFiles(fileDTOList);

        // 3) 하위 폴더(자식)
        //    folder.getSubFolders()로 가져오거나, folderRepository.findByParentFolder(folder) 등
        List<Folder> children = folder.getSubFolders();
        List<FolderPythonRequestDTO> subFolderDTOs = children.stream()
                .map(this::buildFolderDTO)  // 재귀 호출
                .toList();
        dto.setSubFolders(subFolderDTOs);

        return dto;
    }
}