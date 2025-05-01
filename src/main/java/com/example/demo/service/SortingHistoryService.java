package com.example.demo.service;

import com.example.demo.dto.sortingHistoryDTO.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.demo.repository.ImportantBinRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SortingHistoryService {

    private final SortingHistoryRepository sortingHistoryRepository;
    private final FileSortingHistoryRepository fileSortingHistoryRepository;
    private final FolderSortingHistoryRepository folderSortingHistoryRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final FolderAccessRepository folderAccessRepository;
    private final ImportantBinRepository importantBinRepository;

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
                .isMaintain(request.getIsMaintain())
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

            Folder newFolder = file.getFolder();
            Folder previousFolder = folderRepository.findById(fileDTO.getPreviousFolderId())
                    .orElseThrow(() -> new RuntimeException("이전 폴더를 찾을 수 없습니다."));

            // 정리 이력 저장
            FileSortingHistory fileSorting = FileSortingHistory.builder()
                    .file(file)
                    .sorting(sorting)
                    .previousFolder(previousFolder)
                    .newFolder(newFolder)
                    .previousFilePath(fileDTO.getPreviousFilePath())
                    .build();
            fileSortingHistoryRepository.save(fileSorting);
        }
    }

    @Transactional
    public void rollbackSortingHistory(Long sortingId) {
        SortingHistory sortingHistory = sortingHistoryRepository.findById(sortingId)
                .orElseThrow(() -> new RuntimeException("정리 기록이 존재하지 않습니다."));

        if (sortingHistory.getIsMaintain()) {
            // 유지하는 경우 (isMaintain = true)
            System.out.println("isMaintain=true");
            rollbackWhenMaintain(sortingId);
        } else {
            // 유지하지 않는 경우 (isMaintain = false)
            System.out.println("isMaintain=false");
            rollbackWhenNotMaintain(sortingId);
        }
    }

    @Transactional
    public void rollbackWhenNotMaintain(Long sortingId) {

        // 1. 삭제됐던 폴더 복구
        folderSortingHistoryRepository.findBySortingIdAndStatus(sortingId, FolderStatus.DELETED).forEach(record -> {
            Folder folder = record.getFolder();
            Folder parentFolder = folder.getParentFolder();
            String originalName = folder.getName();
            originalName = originalName.replaceAll("\\(\\d+\\)$", ""); // 끝에 붙은 (숫자) 제거
            String newName = originalName;

            int suffix = 1;

            while (folderRepository.existsByParentFolderIdAndNameAndIdNotAndIsDeletedFalse(parentFolder.getId(), newName, folder.getId())) {
                newName = originalName + "(" + suffix + ")";
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
            if (originalName.toLowerCase().endsWith("." + fileType)) {
                baseName = originalName.substring(0, originalName.length() - (fileType.length() + 1));
            }
            baseName = baseName.replaceAll("\\(\\d+\\)$", ""); // 끝에 붙은 (숫자) 제거

            String candidateName = baseName + "." + fileType;
            int suffix = 1;

            while (fileRepository.existsByFolderIdAndNameAndIdNotAndIsDeletedFalse(previousFolder.getId(), candidateName, file.getId())) {
                candidateName = baseName + "(" + suffix + ")." + fileType;
                suffix++;
            }

            file.setName(candidateName);
            file.setFolder(previousFolder);

            // 경로 복원
            String previousPath = record.getPreviousFilePath();
            if (previousPath != null && !previousPath.isBlank()) {
                int lastSlashIndex = previousPath.lastIndexOf('/');
                if (lastSlashIndex != -1) {
                    String newPath = previousPath.substring(0, lastSlashIndex + 1) + candidateName;
                    file.setFilePath(newPath);
                }
            }

            fileRepository.save(file);
        });

        // 3. 생성됐던 폴더 삭제
        // 1. 먼저 CREATED 상태의 folder들을 리스트로 저장
        List<Folder> foldersToDelete = folderSortingHistoryRepository
                .findBySortingIdAndStatus(sortingId, FolderStatus.CREATED)
                .stream()
                .map(FolderSortingHistory::getFolder)
                .sorted((f1, f2) -> Integer.compare(getFolderDepth(f2), getFolderDepth(f1))) // 깊이 내림차순 정렬
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
        for (Long id : folderIdsToDelete) {
            // ✅ (1) folder_access 먼저 삭제
            folderAccessRepository.deleteAllByFolderId(id);
            // (2) important_bin 먼저 삭제
            importantBinRepository.deleteAllByFolderId(id);

            boolean hasFiles = fileRepository.existsByFolderIdAndIsDeletedFalse(id);
            boolean hasSubFolders = folderRepository.existsByParentFolderIdAndIsDeletedFalse(id);

            if (!hasFiles && !hasSubFolders) {
                folderRepository.deleteByFolderId(id);
            }
        }

        // 4. sorting_history에서 기록 삭제
        sortingHistoryRepository.deleteBySortingId(sortingId);
        sortingHistoryRepository.flush();
    }

    @Transactional
    public void rollbackWhenMaintain(Long sortingId) {
        // 1. 먼저 삭제할 복제 파일들 리스트로 확보 (history 삭제 전에)
        List<File> filesToDelete = fileSortingHistoryRepository.findBySortingId(sortingId)
                .stream()
                .map(FileSortingHistory::getFile)
                .toList();

        // 2. file_sorting_history 삭제
        fileSortingHistoryRepository.deleteAllBySortingId(sortingId);
        fileSortingHistoryRepository.flush();

        // 3. 복제된 파일 실제 삭제
        for (File file : filesToDelete) {
            // 중요문서함에서 먼저 삭제
            Long id = file.getId();
            importantBinRepository.deleteAllByFileId(id);
            importantBinRepository.flush();

            fileRepository.deleteByFileId(id);
            fileRepository.flush();
        }

        // 4. 먼저 CREATED 상태의 folder들을 리스트로 저장
        List<Folder> foldersToDelete = folderSortingHistoryRepository
                .findBySortingIdAndStatus(sortingId, FolderStatus.CREATED)
                .stream()
                .map(FolderSortingHistory::getFolder)
                .sorted((f1, f2) -> Integer.compare(getFolderDepth(f2), getFolderDepth(f1))) // 깊이 내림차순 정렬
                .toList();

        // 5. folder_sorting_history 전체 삭제 (이제 folder는 참조 안 됨)
        folderSortingHistoryRepository.deleteAllBySortingId(sortingId);
        folderSortingHistoryRepository.flush();

        // 6. folders 직접 삭제 (ID 기반)
        List<Long> folderIdsToDelete = foldersToDelete.stream()
                .map(Folder::getId)
                .toList();
        for (Long id : folderIdsToDelete) {
            // ✅ (1) folder_access 먼저 삭제
            folderAccessRepository.deleteAllByFolderId(id);
            // (2) important_bin 먼저 삭제
            importantBinRepository.deleteAllByFolderId(id);

            boolean hasFiles = fileRepository.existsByFolderIdAndIsDeletedFalse(id);
            boolean hasSubFolders = folderRepository.existsByParentFolderIdAndIsDeletedFalse(id);

            if (!hasFiles && !hasSubFolders) {
                folderRepository.deleteByFolderId(id);
                folderRepository.flush();
            }
        }

        // 7. sorting_history에서 기록 삭제
        sortingHistoryRepository.deleteBySortingId(sortingId);
        sortingHistoryRepository.flush();

        System.out.println("📁 삭제 대상 파일: " + filesToDelete.size());
        System.out.println("📁 삭제 대상 폴더: " + folderIdsToDelete);
    }

    private int getFolderDepth(Folder folder) {
        int depth = 0;
        Folder current = folder.getParentFolder();
        while (current != null) {
            depth++;
            current = current.getParentFolder();
        }
        return depth;
    }

    @Transactional
    public List<SortingHistoryResponseDTO> getSortingHistoryFiles(Long userId) {
        List<SortingHistory> historyList = sortingHistoryRepository.findAllByUserIdOrderBySortedAtDesc(userId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return historyList.stream()
                .map(history -> {
                    LocalDateTime localDateTime = history.getSortedAt().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();

                    return new SortingHistoryResponseDTO(
                            String.valueOf(history.getId()),
                            history.getUser().getId(),
                            localDateTime.format(formatter)
                    );
                })
                .collect(Collectors.toList());
    }

    public SortingHistorySelectedResponseDTO getSortingHistorySelectedFiles(Long sortingId) {
        List<FileSortingHistory> fileHistories = fileSortingHistoryRepository.findBySortingId(sortingId);

        List<SortingHistoryFileResponseDTO> fileResponses = fileHistories.stream().map(history -> {
            File file = history.getFile();
            Folder previousFolder = history.getPreviousFolder();
            Folder currentFolder = file.getFolder();

            return new SortingHistoryFileResponseDTO(
                    previousFolder.getName(),
                    history.getPreviousFilePath(),
                    currentFolder.getName(),
                    file.getFilePath()
            );
        }).toList();

        return new SortingHistorySelectedResponseDTO(fileResponses);
    }

    public Long getLatestSortingHistoryId(Long userId) {
        return sortingHistoryRepository.findTopByUserIdOrderBySortedAtDesc(userId)
                .map(SortingHistory::getId)
                .orElseThrow(() -> new RuntimeException("정리 기록이 없습니다."));
    }
}
