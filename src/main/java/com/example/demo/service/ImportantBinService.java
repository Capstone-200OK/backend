package com.example.demo.service;

import com.example.demo.dto.importantBinDTO.*;
import com.example.demo.entity.File;
import com.example.demo.entity.Folder;
import com.example.demo.entity.ImportantBin;
import com.example.demo.entity.User;
import com.example.demo.repository.FileRepository;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.ImportantBinRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImportantBinService {

    private final ImportantBinRepository importantBinRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addToImportantBin(ImportantBinRequestDTO requestDTO) {
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (requestDTO.getFileId() != null) {
            File file = fileRepository.findById(requestDTO.getFileId())
                    .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));

            // ✅ 중복 체크
            if (importantBinRepository.existsByUserAndFile(user, file)) {
                throw new IllegalStateException("이미 중요 문서함에 추가된 파일입니다.");
            }

            file.setIsImportant(true);

            ImportantBin importantBin = ImportantBin.builder()
                    .user(user)
                    .file(file)
                    .build();

            importantBinRepository.save(importantBin);

        } else if (requestDTO.getFolderId() != null) {
            Folder folder = folderRepository.findById(requestDTO.getFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("폴더를 찾을 수 없습니다."));

            if (importantBinRepository.existsByUserAndFolder(user, folder)) {
                throw new IllegalStateException("이미 중요 문서함에 추가된 폴더입니다.");
            }

            folder.setIsImportant(true);

            ImportantBin importantBin = ImportantBin.builder()
                    .user(user)
                    .folder(folder)
                    .build();

            importantBinRepository.save(importantBin);
        } else {
            throw new IllegalArgumentException("파일 또는 폴더 ID 중 하나는 필요합니다.");
        }
    }

    @Transactional
    public void removeFromImportantBin(Long importantId) {
        // ImportantBin에서 항목 조회
        ImportantBin importantBin = importantBinRepository.findById(importantId)
                .orElseThrow(() -> new IllegalArgumentException("중요 문서함에서 해당 항목을 찾을 수 없습니다."));

        // 파일 또는 폴더에서 중요 표시 해제
        if (importantBin.getFile() != null) {
            importantBin.getFile().setIsImportant(false);
        } else if (importantBin.getFolder() != null) {
            importantBin.getFolder().setIsImportant(false);
        }

        // ImportantBin 항목 삭제
        importantBinRepository.delete(importantBin);
    }

    @jakarta.transaction.Transactional
    public List<ImportantBinFileResponseDTO> getImportantFiles(Long userId) {
        return importantBinRepository.findAll().stream()
                .filter(important -> important.getUser().getId().equals(userId))
                .filter(important -> important.getFile() != null)
                .map(important -> ImportantBinFileResponseDTO.builder()
                        .importantId(important.getId())
                        .fileId(important.getFile().getId())
                        .fileName(important.getFile().getName())
                        .fileType(important.getFile().getFileType())
                        .size(important.getFile().getSize())
                        .build())
                .collect(Collectors.toList());
    }

    @jakarta.transaction.Transactional
    public List<ImportantBinFolderResponseDTO> getImportantFolders(Long userId) {
        return importantBinRepository.findAll().stream()
                .filter(important -> important.getUser().getId().equals(userId))
                .filter(important -> important.getFolder() != null)
                .map(important -> ImportantBinFolderResponseDTO.builder()
                        .importantId(important.getId())
                        .folderId(important.getFolder().getId())
                        .folderName(important.getFolder().getName())
                        .build())
                .collect(Collectors.toList());
    }
}