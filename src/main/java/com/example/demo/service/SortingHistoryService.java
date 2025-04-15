package com.example.demo.service;

import com.example.demo.dto.sortingHistoryDTO.SortingHistoryRequestDTO;
import com.example.demo.dto.fileDTO.FileUpdateRequestDTO;
import com.example.demo.dto.folderDTO.FolderUpdateRequestDTO;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SortingHistoryService {

    private final SortingHistoryRepository sortingHistoryRepository;
    private final FileSortingHistoryRepository fileSortingHistoryRepository;
    private final FolderSortingHistoryRepository folderSortingHistoryRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;

    @Transactional
    public void saveSortingHistory(SortingHistoryRequestDTO request) {
        Long userId = request.getUserId();
        List<FileUpdateRequestDTO> fileUpdates = request.getFileUpdates();
        List<FolderUpdateRequestDTO> folderUpdates = request.getFolderUpdates();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 정리 기록 생성 및 저장
        SortingHistory sorting = SortingHistory.builder()
                .user(user)
                .build();
        sortingHistoryRepository.save(sorting);

        // 폴더 정리 처리
        for (FolderUpdateRequestDTO folderDTO : folderUpdates) {
            Folder folder = folderRepository.findById(folderDTO.getFolderId())
                    .orElseThrow(() -> new RuntimeException("폴더를 찾을 수 없습니다."));

            FolderStatus status = folderDTO.getStatus();
            // 상태 적용
            if (folderDTO.getStatus() == FolderStatus.DELETED) {
                folder.setIsDeleted(true);
            }

            FolderSortingHistory folderSorting = FolderSortingHistory.builder()
                    .folder(folder)
                    .sorting(sorting)
                    .status(status)
                    .build();
            folderSortingHistoryRepository.save(folderSorting);
        }

        // 파일 정리 처리
        for (FileUpdateRequestDTO fileDTO : fileUpdates) {
            File file = fileRepository.findById(fileDTO.getFileId())
                    .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

            Folder previousFolder = file.getFolder();
            Folder newFolder = folderRepository.findById(fileDTO.getNewFolderId())
                    .orElseThrow(() -> new RuntimeException("새 폴더를 찾을 수 없습니다."));

            // 변경사항 적용
            file.setName(fileDTO.getNewName());
            file.setFilePath(fileDTO.getNewFilePath() + fileDTO.getNewName());
            file.setFolder(newFolder);

            // 정리 이력 저장
            FileSortingHistory fileSorting = FileSortingHistory.builder()
                    .file(file)
                    .sorting(sorting)
                    .previousFolder(previousFolder)
                    .newFolder(newFolder)
                    .build();
            fileSortingHistoryRepository.save(fileSorting);
        }
    }

    @Transactional
    public void rollbackSortingHistory(Long sortingId) {
        SortingHistory sorting = sortingHistoryRepository.findById(sortingId)
                .orElseThrow(() -> new RuntimeException("정리 기록이 존재하지 않습니다."));

        // 1. 삭제됐던 폴더 복구
        folderSortingHistoryRepository.findBySortingIdAndStatus(sortingId, FolderStatus.DELETED).forEach(record -> {
            Folder folder = record.getFolder();
            Folder parentFolder = folder.getParentFolder();
            String originalName = folder.getName();
            String newName = originalName;
            int suffix = 1;

            while (folderRepository.existsByUserIdAndParentFolderAndName(folder.getUser().getId(), parentFolder, newName)) {
                newName = originalName + " (" + suffix + ")";
                suffix++;
            }

            folder.setName(newName);
            folder.setIsDeleted(false);
            folderRepository.save(folder);
        });

        // 2. 파일 원래 위치로 되돌리기
        fileSortingHistoryRepository.findBySortingId(sortingId).forEach(record -> {
            File file = record.getFile();
            Folder previousFolder = record.getPreviousFolder();

            String originalName = file.getName();
            String fileType = file.getFileType();
            String baseName = originalName;
            int suffix = 1;

            while (fileRepository.existsByFolderAndNameAndFileType(previousFolder, baseName, fileType)) {
                baseName = originalName + " (" + suffix + ")";
                suffix++;
            }

            file.setName(baseName);
            file.setFolder(previousFolder);
            // 경로 복원하기
            fileRepository.save(file);
        });

        // 3. 생성됐던 폴더 삭제
        // 1. 먼저 CREATED 상태의 folder들을 리스트로 저장
        List<Folder> foldersToDelete = folderSortingHistoryRepository
                .findBySortingIdAndStatus(sortingId, FolderStatus.CREATED)
                .stream()
                .map(FolderSortingHistory::getFolder)
                .toList();

        // 2. file_sorting_history 전체 삭제 (이제 folder는 참조 안 됨)
        fileSortingHistoryRepository.deleteAllBySortingId(sortingId);
        fileSortingHistoryRepository.flush();

        // 2. folder_sorting_history 전체 삭제 (이제 folder는 참조 안 됨)
        folderSortingHistoryRepository.deleteAllBySortingId(sortingId);
        folderSortingHistoryRepository.flush();

        // 3. folders 직접 삭제 (ID 기반)
        List<Long> folderIdsToDelete = foldersToDelete.stream()
                .map(Folder::getId)
                .toList();
        folderRepository.deleteAll(folderIdsToDelete);

        // 4. sorting_history에서 기록 삭제
        sortingHistoryRepository.deleteBySortingId(sortingId);
        sortingHistoryRepository.flush();
    }
}
