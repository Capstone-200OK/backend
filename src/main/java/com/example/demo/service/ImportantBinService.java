package com.example.demo.service;

import com.example.demo.dto.importantBinDTO.ImportantBinFileResponseDTO;
import com.example.demo.dto.importantBinDTO.ImportantBinFolderResponseDTO;
import com.example.demo.dto.importantBinDTO.ImportantBinRequestDTO;
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

    /**
     * 중요 문서함에 파일 또는 폴더를 추가하는 메서드
     *
     * @param requestDTO 추가할 파일 또는 폴더 정보가 담긴 DTO
     */
    @Transactional
    public void addToImportantBin(ImportantBinRequestDTO requestDTO) {
        // 사용자 조회
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 파일 추가 로직
        if (requestDTO.getFileId() != null) {
            // 파일 조회
            File file = fileRepository.findById(requestDTO.getFileId())
                    .orElseThrow(() -> new IllegalArgumentException("File not found"));

            // 중복 체크
            if (importantBinRepository.existsByUserAndFile(user, file)) {
                throw new IllegalStateException("File already added to important bin");
            }

            // 파일 중요 표시
            file.setIsImportant(true);

            // 중요 문서함 엔티티 생성 및 저장
            ImportantBin importantBin = ImportantBin.builder()
                    .user(user)
                    .file(file)
                    .build();

            importantBinRepository.save(importantBin);

            // 폴더 추가 로직
        } else if (requestDTO.getFolderId() != null) {
            // 폴더 조회
            Folder folder = folderRepository.findById(requestDTO.getFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

            // 중복 체크
            if (importantBinRepository.existsByUserAndFolder(user, folder)) {
                throw new IllegalStateException("Folder already added to important bin");
            }

            // 폴더 중요 표시
            folder.setIsImportant(true);

            // 중요 문서함 엔티티 생성 및 저장
            ImportantBin importantBin = ImportantBin.builder()
                    .user(user)
                    .folder(folder)
                    .build();

            importantBinRepository.save(importantBin);
        } else {
            // 파일 또는 폴더 ID가 모두 없는 경우 예외 처리
            throw new IllegalArgumentException("Either fileId or folderId must be provided");
        }
    }

    /**
     * 중요 문서함에서 파일 또는 폴더를 제거하는 메서드
     *
     * @param importantId 제거할 중요 문서함 항목의 ID
     */
    @Transactional
    public void removeFromImportantBin(Long importantId) {
        // 중요 문서함 항목 조회
        ImportantBin importantBin = importantBinRepository.findById(importantId)
                .orElseThrow(() -> new IllegalArgumentException("Important bin item not found"));

        // 파일 또는 폴더의 중요 표시 해제
        if (importantBin.getFile() != null) {
            importantBin.getFile().setIsImportant(false);
        } else if (importantBin.getFolder() != null) {
            importantBin.getFolder().setIsImportant(false);
        }

        // 중요 문서함 항목 삭제
        importantBinRepository.delete(importantBin);
    }

    /**
     * 사용자의 중요 문서함에 있는 파일 목록을 조회하는 메서드
     *
     * @param userId 조회할 사용자 ID
     * @return 중요 파일 리스트
     */
    @Transactional
    public List<ImportantBinFileResponseDTO> getImportantFiles(Long userId) {
        // 사용자 ID로 필터링 후 파일이 존재하는 항목만 반환
        return importantBinRepository.findAll().stream()
                .filter(important -> important.getUser().getId().equals(userId))
                .filter(important -> important.getFile() != null)
                // 엔티티를 DTO로 변환
                .map(important -> ImportantBinFileResponseDTO.builder()
                        .importantId(important.getId())
                        .fileId(important.getFile().getId())
                        .fileName(important.getFile().getName())
                        .fileType(important.getFile().getFileType())
                        .size(important.getFile().getSize())
                        .fileUrl(important.getFile().getFileUrl())
                        .fileThumbnailUrl(important.getFile().getFileThumbUrl())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 중요 문서함에 있는 폴더 목록을 조회하는 메서드
     *
     * @param userId 조회할 사용자 ID
     * @return 중요 폴더 리스트
     */
    @Transactional
    public List<ImportantBinFolderResponseDTO> getImportantFolders(Long userId) {
        // 사용자 ID로 필터링 후 폴더가 존재하고 삭제되지 않은 항목만 반환
        return importantBinRepository.findAll().stream()
                .filter(important -> important.getUser().getId().equals(userId))
                .filter(important -> important.getFolder() != null)
                .filter(important -> !important.getFolder().getIsDeleted())
                // 엔티티를 DTO로 변환
                .map(important -> ImportantBinFolderResponseDTO.builder()
                        .importantId(important.getId())
                        .folderId(important.getFolder().getId())
                        .folderName(important.getFolder().getName())
                        .build())
                .collect(Collectors.toList());
    }
}
