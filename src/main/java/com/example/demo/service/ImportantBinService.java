package com.example.demo.service;

import com.example.demo.dto.importantBinDTO.ImportantBinRequestDTO;
import com.example.demo.dto.importantBinDTO.ImportantBinResponseDTO;
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

        File file = null;
        Folder folder = null;

        if (requestDTO.getFileId() != null) {
            file = fileRepository.findById(requestDTO.getFileId())
                    .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));
            file.setIsImportant(true);
        } else if (requestDTO.getFolderId() != null) {
            folder = folderRepository.findById(requestDTO.getFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("폴더를 찾을 수 없습니다."));
            folder.setIsImportant(true);
        } else {
            throw new IllegalArgumentException("파일 또는 폴더 ID 중 하나는 필요합니다.");
        }

        ImportantBin importantBin = ImportantBin.builder()
                .user(user)
                .file(file)
                .folder(folder)
                .build();

        importantBinRepository.save(importantBin);
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

    @Transactional(readOnly = true)
    public List<ImportantBinResponseDTO> getImportantListByUser(Long userId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 해당 사용자의 중요 문서함 목록 조회
        List<ImportantBin> importantList = importantBinRepository.findAllByUser(user);

        // 엔티티 -> DTO 변환
        return importantList.stream()
                .map(importantBin -> ImportantBinResponseDTO.builder()
                        .importantId(importantBin.getId())
                        .fileId(importantBin.getFile() != null ? importantBin.getFile().getId() : null)
                        .folderId(importantBin.getFolder() != null ? importantBin.getFolder().getId() : null)
                        .build())
                .collect(Collectors.toList());
    }
}