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
import java.util.ArrayList;
import java.util.Collections;
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
    private final FolderAccessService folderAccessService;

    /**
     * 자동 분류 기록 저장
     *
     * @param request 자동 분류 요청 DTO
     */
    @Transactional
    public void saveSortingHistory(SortingHistoryRequestDTO request) {
        Long userId = request.getUserId();
        List<FileUpdateRequestDTO> fileUpdates = request.getFileUpdates();
        List<FolderUpdateRequestDTO> folderUpdates = request.getFolderUpdates();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 정리 기록 생성 및 저장
        SortingHistory sorting = SortingHistory.builder()
                .user(user)
                .isMaintain(request.getIsMaintain())
                .build();
        sortingHistoryRepository.save(sorting);

        // 폴더 변경 기록 저장
        for (FolderUpdateRequestDTO folderDTO : folderUpdates) {
            Folder folder = folderRepository.findById(folderDTO.getFolderId())
                    .orElseThrow(() -> new RuntimeException("Folder not found"));

            if (folderDTO.getStatus() == FolderStatus.DELETED) {
                folder.setIsDeleted(true);
            }

            FolderSortingHistory folderSorting = FolderSortingHistory.builder()
                    .folder(folder)
                    .sorting(sorting)
                    .status(folderDTO.getStatus())
                    .build();
            folderSortingHistoryRepository.save(folderSorting);
        }

        // 파일 변경 기록 저장
        for (FileUpdateRequestDTO fileDTO : fileUpdates) {
            File file = fileRepository.findById(fileDTO.getFileId())
                    .orElseThrow(() -> new RuntimeException("File not found"));

            Folder newFolder = file.getFolder();
            Folder previousFolder = folderRepository.findById(fileDTO.getPreviousFolderId())
                    .orElseThrow(() -> new RuntimeException("Previous folder not found"));

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

    /**
     * 자동 분류 복구
     *
     * @param sortingId 복구할 자동 분류 기록 ID
     */
    @Transactional
    public void rollbackSortingHistory(Long sortingId) {
        SortingHistory sortingHistory = sortingHistoryRepository.findById(sortingId)
                .orElseThrow(() -> new RuntimeException("Sorting history not found"));

        // isMaintain 값에 따라 다른 로직 실행
        if (sortingHistory.getIsMaintain()) {
            rollbackWhenMaintain(sortingId);
        } else {
            rollbackWhenNotMaintain(sortingId);
        }
    }

    /**
     * 유지(false)일 때 자동 분류 복구
     *
     * @param sortingId 자동 분류 기록 ID
     */
    @Transactional
    public void rollbackWhenNotMaintain(Long sortingId) {
        // 삭제됐던 폴더 복구
        folderSortingHistoryRepository.findBySortingIdAndStatus(sortingId, FolderStatus.DELETED).forEach(record -> {
            Folder folder = record.getFolder();
            Folder parentFolder = folder.getParentFolder();
            String originalName = folder.getName().replaceAll("\\(\\d+\\)$", "");
            String newName = originalName;
            int suffix = 1;

            // 중복 이름 방지
            while (folderRepository.existsByParentFolderIdAndNameAndIdNotAndIsDeletedFalse(parentFolder.getId(), newName, folder.getId())) {
                newName = originalName + "(" + suffix++ + ")";
            }

            folder.setName(newName);
            folder.setIsDeleted(false);
            folderRepository.save(folder);
        });

        // 파일 원래 위치로 이동
        fileSortingHistoryRepository.findBySortingId(sortingId).forEach(record -> {
            File file = record.getFile();
            Folder previousFolder = record.getPreviousFolder();
            String originalName = file.getName();
            String fileType = file.getFileType();

            String baseName = originalName;
            if (originalName.toLowerCase().endsWith("." + fileType)) {
                baseName = originalName.substring(0, originalName.length() - (fileType.length() + 1));
            }
            baseName = baseName.replaceAll("\\(\\d+\\)$", "");

            String candidateName = baseName + "." + fileType;
            int suffix = 1;
            while (fileRepository.existsByFolderIdAndNameAndIdNotAndIsDeletedFalse(previousFolder.getId(), candidateName, file.getId())) {
                candidateName = baseName + "(" + suffix++ + ")." + fileType;
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

        // CREATED 상태 폴더 삭제
        List<Folder> foldersToDelete = folderSortingHistoryRepository
                .findBySortingIdAndStatus(sortingId, FolderStatus.CREATED)
                .stream()
                .map(FolderSortingHistory::getFolder)
                .sorted((f1, f2) -> Integer.compare(getFolderDepth(f2), getFolderDepth(f1)))
                .toList();

        fileSortingHistoryRepository.deleteAllBySortingId(sortingId);
        fileSortingHistoryRepository.flush();

        folderSortingHistoryRepository.deleteAllBySortingId(sortingId);
        folderSortingHistoryRepository.flush();

        // 폴더 삭제
        for (Folder folder : foldersToDelete) {
            Long id = folder.getId();
            folderAccessRepository.deleteAllByFolderId(id);
            importantBinRepository.deleteAllByFolderId(id);

            boolean hasFiles = fileRepository.existsByFolderIdAndIsDeletedFalse(id);
            boolean hasSubFolders = folderRepository.existsByParentFolderIdAndIsDeletedFalse(id);
            if (!hasFiles && !hasSubFolders) {
                folderRepository.deleteByFolderId(id);
            }
        }

        sortingHistoryRepository.deleteBySortingId(sortingId);
        sortingHistoryRepository.flush();
    }

    /**
     * 유지(true)일 때 자동 분류 복구
     *
     * @param sortingId 자동 분류 기록 ID
     */
    @Transactional
    public void rollbackWhenMaintain(Long sortingId) {
        // 삭제할 파일 리스트 확보
        List<File> filesToDelete = fileSortingHistoryRepository.findBySortingId(sortingId)
                .stream()
                .map(FileSortingHistory::getFile)
                .toList();

        fileSortingHistoryRepository.deleteAllBySortingId(sortingId);
        fileSortingHistoryRepository.flush();

        // 파일 삭제
        for (File file : filesToDelete) {
            Long id = file.getId();
            importantBinRepository.deleteAllByFileId(id);
            importantBinRepository.flush();
            fileRepository.deleteByFileId(id);
            fileRepository.flush();
        }

        // CREATED 상태 폴더 삭제
        List<Folder> foldersToDelete = folderSortingHistoryRepository
                .findBySortingIdAndStatus(sortingId, FolderStatus.CREATED)
                .stream()
                .map(FolderSortingHistory::getFolder)
                .sorted((f1, f2) -> Integer.compare(getFolderDepth(f2), getFolderDepth(f1)))
                .toList();

        folderSortingHistoryRepository.deleteAllBySortingId(sortingId);
        folderSortingHistoryRepository.flush();

        for (Folder folder : foldersToDelete) {
            Long id = folder.getId();
            folderAccessRepository.deleteAllByFolderId(id);
            importantBinRepository.deleteAllByFolderId(id);

            boolean hasFiles = fileRepository.existsByFolderIdAndIsDeletedFalse(id);
            boolean hasSubFolders = folderRepository.existsByParentFolderIdAndIsDeletedFalse(id);
            if (!hasFiles && !hasSubFolders) {
                folderRepository.deleteByFolderId(id);
                folderRepository.flush();
            }
        }

        sortingHistoryRepository.deleteBySortingId(sortingId);
        sortingHistoryRepository.flush();
    }

    /**
     * 폴더 깊이 계산
     *
     * @param folder 폴더 엔티티
     * @return 깊이
     */
    private int getFolderDepth(Folder folder) {
        int depth = 0;
        Folder current = folder.getParentFolder();
        while (current != null) {
            depth++;
            current = current.getParentFolder();
        }
        return depth;
    }

    /**
     * 사용자 자동 분류 기록 조회
     *
     * @param userId 사용자 ID
     * @return 자동 분류 기록 리스트
     */
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

    /**
     * 특정 자동 분류 기록의 파일 이동 정보 조회
     *
     * @param sortingId 자동 분류 기록 ID
     * @return 파일 이동 정보 DTO
     */
    public SortingHistorySelectedResponseDTO getSortingHistorySelectedFiles(Long sortingId, Long userId) {
        List<FileSortingHistory> fileHistories = fileSortingHistoryRepository.findBySortingId(sortingId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<SortingHistoryFileResponseDTO> fileResponses = fileHistories.stream().map(history -> {
            File file = history.getFile();
            Folder previousFolder = history.getPreviousFolder();
            Folder currentFolder = file.getFolder();

            // 1. 유저가 접근할 수 있는 root 기준 찾기
            Folder prevRoot = folderAccessService.findTopAccessibleRoot(user, previousFolder);
            Folder currRoot = folderAccessService.findTopAccessibleRoot(user, currentFolder);

            // 2. 각각 fullPath 만들기 (CloudRoot/부터 시작)
            String prevPath = "CloudRoot/" + buildUserVisiblePath(previousFolder, prevRoot) + "/" + file.getName();
            String currPath = "CloudRoot/" + buildUserVisiblePath(currentFolder, currRoot) + "/" + file.getName();

            return new SortingHistoryFileResponseDTO(
                    previousFolder.getName(),
                    prevPath,
                    currentFolder.getName(),
                    currPath
            );
        }).toList();

        return new SortingHistorySelectedResponseDTO(fileResponses);
    }


    private String buildUserVisiblePath(Folder folder, Folder rootFolder) {
        List<String> parts = new ArrayList<>();

        Folder current = folder;
        while (current != null && !current.getId().equals(rootFolder.getParentFolder() != null ? rootFolder.getParentFolder().getId() : null)) {
            parts.add(current.getName());
            current = current.getParentFolder();
        }

        Collections.reverse(parts); // 상위 -> 하위 순서로
        return String.join("/", parts);
    }

    /**
     * 가장 최근 자동 분류 기록 ID 조회
     *
     * @param userId 사용자 ID
     * @return 최근 자동 분류 기록 ID
     */
    public Long getLatestSortingHistoryId(Long userId) {
        return sortingHistoryRepository.findTopByUserIdOrderBySortedAtDesc(userId)
                .map(SortingHistory::getId)
                .orElseThrow(() -> new RuntimeException("정리 기록이 없습니다."));
    }
}
