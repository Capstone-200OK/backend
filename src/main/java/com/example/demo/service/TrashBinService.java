package com.example.demo.service;

import com.example.demo.dto.trashBinDTO.TrashBinFileResponseDTO;
import com.example.demo.dto.trashBinDTO.TrashBinFolderResponseDTO;
import com.example.demo.dto.trashBinDTO.TrashBinRequestDTO;
import com.example.demo.entity.File;
import com.example.demo.entity.Folder;
import com.example.demo.entity.TrashBin;
import com.example.demo.entity.User;
import com.example.demo.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrashBinService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final TrashBinRepository trashBinRepository;
    private final UserRepository userRepository;
    private final FolderAccessRepository folderAccessRepository;
    private final ImportantBinRepository importantBinRepository;
    private final FileSortingHistoryRepository fileSortingHistoryRepository;
    private final FolderSortingHistoryRepository folderSortingHistoryRepository;
    private final ScheduledTaskRepository scheduledTaskRepository;

    /**
     * 파일 및 폴더를 휴지통으로 이동
     *
     * @param dto 파일 및 폴더 ID, 사용자 ID를 담고 있는 DTO
     */
    @Transactional
    public void moveToTrash(TrashBinRequestDTO dto) {
        // 사용자 조회
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 파일 삭제 처리
        if (dto.getFileIds() != null) {
            for (Long fileId : dto.getFileIds()) {
                File file = fileRepository.findById(fileId)
                        .orElseThrow(() -> new RuntimeException("File not found"));

                // 상위 폴더가 삭제된 경우 예외
                Folder parent = file.getFolder();
                if (parent != null && Boolean.TRUE.equals(parent.getIsDeleted())) {
                    throw new IllegalStateException("Cannot delete file in deleted parent folder");
                }

                // 이미 삭제된 파일 예외
                if (Boolean.TRUE.equals(file.getIsDeleted())) {
                    throw new IllegalStateException("File is already deleted");
                }

                // 파일 삭제 상태 변경
                file.setIsDeleted(true);
                fileRepository.save(file);

                // 휴지통 엔티티 생성
                TrashBin trash = TrashBin.builder()
                        .user(user)
                        .file(file)
                        .build();
                trashBinRepository.save(trash);
            }
        }

        // 폴더 삭제 처리
        if (dto.getFolderIds() != null) {
            for (Long folderId : dto.getFolderIds()) {
                Folder folder = folderRepository.findById(folderId)
                        .orElseThrow(() -> new RuntimeException("Folder not found"));

                // 상위 폴더가 삭제된 경우 또는 이미 삭제된 폴더는 무시
                if (folder.getParentFolder() != null && Boolean.TRUE.equals(folder.getParentFolder().getIsDeleted())) {
                    continue;
                }
                if (Boolean.TRUE.equals(folder.getIsDeleted())) {
                    continue;
                }

                // 폴더 및 하위 파일/폴더 삭제 상태 변경
                markFolderAndContentsAsDeleted(folder);

                // 휴지통 엔티티 생성
                TrashBin trash = TrashBin.builder()
                        .user(user)
                        .folder(folder)
                        .build();
                trashBinRepository.save(trash);
            }
        }
    }

    /**
     * 폴더 및 하위 파일/폴더 삭제 상태로 변경 (재귀 호출)
     *
     * @param folder 삭제할 폴더
     */
    private void markFolderAndContentsAsDeleted(Folder folder) {
        folder.setIsDeleted(true);

        List<File> files = fileRepository.findByFolderId(folder.getId());
        files.forEach(file -> file.setIsDeleted(true));

        List<Folder> children = folderRepository.findByParentFolderId(folder.getId());
        children.forEach(this::markFolderAndContentsAsDeleted);
    }

    /**
     * 휴지통 항목들을 복구 (여러 항목)
     *
     * @param trashIds 복구할 휴지통 ID 리스트
     */
    @Transactional
    public void restoreAll(List<Long> trashIds) {
        for (Long id : trashIds) {
            restore(id);
        }
    }

    /**
     * 휴지통 항목 복구 (단일 항목)
     *
     * @param trashId 복구할 휴지통 ID
     */
    @Transactional
    public void restore(Long trashId) {
        TrashBin trash = trashBinRepository.findById(trashId)
                .orElseThrow(() -> new RuntimeException("Trash item not found"));

        // 파일 복구
        if (trash.getFile() != null) {
            File file = trash.getFile();
            Folder parent = file.getFolder();
            if (parent != null && Boolean.TRUE.equals(parent.getIsDeleted())) {
                throw new RuntimeException("Cannot restore file in deleted parent folder");
            }

            // 중복 이름 방지
            String originalName = file.getName();
            String baseName = originalName;
            String extension = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex != -1) {
                baseName = originalName.substring(0, dotIndex);
                extension = originalName.substring(dotIndex);
            }

            String newName = originalName;
            int counter = 1;
            while (parent != null && fileRepository.existsByFolderIdAndNameAndIdNotAndIsDeletedFalse(parent.getId(), newName, file.getId())) {
                newName = baseName + "(" + counter++ + ")" + extension;
            }

            file.setName(newName);
            file.setIsDeleted(false);
        }

        // 폴더 복구
        if (trash.getFolder() != null) {
            Folder folder = trash.getFolder();
            Folder parent = folder.getParentFolder();
            if (parent != null && Boolean.TRUE.equals(parent.getIsDeleted())) {
                throw new RuntimeException("Cannot restore folder in deleted parent folder");
            }

            restoreFolderAndContents(folder);
        }

        // 휴지통 항목 삭제
        trashBinRepository.delete(trash);
    }

    /**
     * 폴더 및 하위 파일/폴더 복구 (재귀 호출)
     *
     * @param folder 복구할 폴더
     */
    private void restoreFolderAndContents(Folder folder) {
        folder.setIsDeleted(false);

        Folder parentFolder = folder.getParentFolder();
        String originalFolderName = folder.getName().replaceAll("\\(\\d+\\)$", "");
        String newFolderName = originalFolderName;
        int folderSuffix = 1;
        while (folderRepository.existsByParentFolderIdAndNameAndIdNotAndIsDeletedFalse(parentFolder.getId(), newFolderName, folder.getId())) {
            newFolderName = originalFolderName + "(" + folderSuffix++ + ")";
        }
        folder.setName(newFolderName);

        // 하위 파일 복구
        List<File> files = fileRepository.findByFolderId(folder.getId());
        for (File file : files) {
            String originalName = file.getName();
            String baseName = originalName;
            String extension = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex != -1) {
                baseName = originalName.substring(0, dotIndex);
                extension = originalName.substring(dotIndex);
            }

            String newName = originalName;
            int counter = 1;
            while (fileRepository.existsByFolderIdAndNameAndIdNotAndIsDeletedFalse(folder.getId(), newName, file.getId())) {
                newName = baseName + "(" + counter++ + ")" + extension;
            }

            file.setName(newName);
            file.setIsDeleted(false);
            fileRepository.save(file);
        }

        // 하위 폴더 재귀 복구
        folderRepository.findByParentFolderId(folder.getId())
                .forEach(this::restoreFolderAndContents);
    }

    /**
     * 휴지통에서 파일 및 폴더 완전 삭제 (여러 항목)
     *
     * @param trashIds 삭제할 휴지통 ID 리스트
     */
    @Transactional
    public void deleteAllPermanently(List<Long> trashIds) {
        for (Long id : trashIds) {
            deletePermanently(id);
        }
    }

    /**
     * 휴지통에서 파일 및 폴더 완전 삭제 (단일 항목)
     *
     * @param trashId 삭제할 휴지통 ID
     */
    public void deletePermanently(Long trashId) {
        TrashBin trash = trashBinRepository.findById(trashId)
                .orElseThrow(() -> new RuntimeException("Trash item not found"));

        if (trash.getFile() != null) {
            fileRepository.delete(trash.getFile());
        }

        if (trash.getFolder() != null) {
            deleteFolderAndContents(trash.getFolder());
        }

        trashBinRepository.delete(trash);
    }

    /**
     * 폴더 및 하위 파일/폴더를 완전 삭제 (재귀 호출)
     *
     * @param folder 삭제할 폴더
     */
    private void deleteFolderAndContents(Folder folder) {
        Long folderId = folder.getId();

        List<File> files = fileRepository.findByFolderId(folderId);
        fileSortingHistoryRepository.deleteAllByFileIn(files);
        importantBinRepository.deleteAllByFileIn(files);
        fileRepository.deleteAll(files);

        folderRepository.findByParentFolderId(folderId)
                .forEach(this::deleteFolderAndContents);

        scheduledTaskRepository.deleteAllByPreviousFolderId(folderId);
        scheduledTaskRepository.deleteAllByNewFolderId(folderId);
        fileSortingHistoryRepository.deleteAllByPreviousFolderId(folderId);
        fileSortingHistoryRepository.deleteAllByNewFolderId(folderId);
        folderSortingHistoryRepository.deleteAllByFolderId(folderId);
        folderAccessRepository.deleteAllByFolderId(folderId);
        importantBinRepository.deleteAllByFolderId(folderId);
        folderRepository.delete(folder);
    }

    /**
     * 휴지통에 있는 파일 리스트 조회
     *
     * @param userId 조회할 사용자 ID
     * @return 파일 리스트
     */
    @Transactional
    public List<TrashBinFileResponseDTO> getDeletedFiles(Long userId) {
        return trashBinRepository.findAll().stream()
                .filter(trash -> trash.getUser().getId().equals(userId))
                .filter(trash -> trash.getFile() != null)
                .map(trash -> TrashBinFileResponseDTO.builder()
                        .trashId(trash.getId())
                        .fileId(trash.getFile().getId())
                        .fileName(trash.getFile().getName())
                        .fileType(trash.getFile().getFileType())
                        .size(trash.getFile().getSize())
                        .deletedAt(trash.getDeletedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 휴지통에 있는 폴더 리스트 조회
     *
     * @param userId 조회할 사용자 ID
     * @return 폴더 리스트
     */
    @Transactional
    public List<TrashBinFolderResponseDTO> getDeletedFolders(Long userId) {
        return trashBinRepository.findAll().stream()
                .filter(trash -> trash.getUser().getId().equals(userId))
                .filter(trash -> trash.getFolder() != null)
                .map(trash -> TrashBinFolderResponseDTO.builder()
                        .trashId(trash.getId())
                        .folderId(trash.getFolder().getId())
                        .folderName(trash.getFolder().getName())
                        .deletedAt(trash.getDeletedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
