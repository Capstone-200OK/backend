package com.example.demo.service;

import com.example.demo.dto.trashBinDTO.*;
import com.example.demo.entity.File;
import com.example.demo.entity.Folder;
import com.example.demo.entity.TrashBin;
import com.example.demo.entity.User;
import com.example.demo.repository.FileRepository;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.TrashBinRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.FolderAccessRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
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

    // 휴지통으로 이동
    @Transactional
    public void moveToTrash(TrashBinRequestDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 파일 처리
        if (dto.getFileIds() != null) {
            for (Long fileId : dto.getFileIds()) {
                File file = fileRepository.findById(fileId)
                        .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

                Folder parent = file.getFolder();
                if (parent != null && Boolean.TRUE.equals(parent.getIsDeleted())) {
                    throw new IllegalStateException("상위 폴더가 삭제된 파일은 직접 삭제할 수 없습니다.");
                }
                if (Boolean.TRUE.equals(file.getIsDeleted())) {
                    throw new IllegalStateException("이미 삭제된 파일입니다.");
                }

                file.setIsDeleted(true);
                fileRepository.save(file);

                TrashBin trash = TrashBin.builder()
                        .user(user)
                        .file(file)
                        .build();

                trashBinRepository.save(trash);
            }
        }

        // 폴더 처리
        if (dto.getFolderIds() != null) {
            for (Long folderId : dto.getFolderIds()) {
                Folder folder = folderRepository.findById(folderId)
                        .orElseThrow(() -> new RuntimeException("폴더를 찾을 수 없습니다."));

                if (folder.getParentFolder() != null && Boolean.TRUE.equals(folder.getParentFolder().getIsDeleted())) {
                    continue;
                }

                if (Boolean.TRUE.equals(folder.getIsDeleted())) {
                    continue;
                }

                markFolderAndContentsAsDeleted(folder);

                TrashBin trash = TrashBin.builder()
                        .user(user)
                        .folder(folder)
                        .build();

                trashBinRepository.save(trash);
            }
        }
    }


    private void markFolderAndContentsAsDeleted(Folder folder) {
        folder.setIsDeleted(true);

        // 파일들 삭제
        List<File> files = fileRepository.findByFolderId(folder.getId());
        for (File file : files) {
            file.setIsDeleted(true);
        }

        // 하위 폴더 재귀 처리
        List<Folder> children = folderRepository.findByParentFolderId(folder.getId());
        for (Folder child : children) {
            markFolderAndContentsAsDeleted(child);
        }
    }

    @Transactional
    public void restoreAll(List<Long> trashIds) {
        for (Long id : trashIds) {
            restore(id);
        }
    }

    @Transactional
    public void restore(Long trashId) {
        TrashBin trash = trashBinRepository.findById(trashId)
                .orElseThrow(() -> new RuntimeException("휴지통 항목이 존재하지 않습니다."));

        if (trash.getFile() != null) {
            File file = trash.getFile();

            Folder parent = file.getFolder();
            if (parent != null && Boolean.TRUE.equals(parent.getIsDeleted())) {
                throw new RuntimeException("상위 폴더가 삭제된 상태에서는 복구할 수 없습니다.");
            }

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

            while (fileRepository.existsByFolderIdAndNameAndIdNotAndIsDeletedFalse(parent.getId(), newName, file.getId())) {
                newName = baseName + "(" + counter + ")" + extension;
                counter++;
            }

            file.setName(newName);
            file.setIsDeleted(false);
        }

        if (trash.getFolder() != null) {
            Folder folder = trash.getFolder();

            Folder parent = folder.getParentFolder();
            if (parent != null && Boolean.TRUE.equals(parent.getIsDeleted())) {
                throw new RuntimeException("상위 폴더가 삭제된 상태에서는 복구할 수 없습니다.");
            }

            restoreFolderAndContents(folder);
        }

        trashBinRepository.delete(trash);
    }

    private void restoreFolderAndContents(Folder folder) {
        folder.setIsDeleted(false);

        // 폴더 이름 충돌 방지 처리
        Folder parentFolder = folder.getParentFolder();
        String originalFolderName = folder.getName();
        originalFolderName = originalFolderName.replaceAll("\\(\\d+\\)$", "");
        String newFolderName = originalFolderName;
        int folderSuffix = 1;

        while (folderRepository.existsByParentFolderIdAndNameAndIdNotAndIsDeletedFalse(parentFolder.getId(), newFolderName, folder.getId())) {
            newFolderName = originalFolderName + "(" + folderSuffix + ")";
            folderSuffix++;
        }

        folder.setName(newFolderName);

        // 파일 복구
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
                newName = baseName + "(" + counter + ")" + extension;
                counter++;
            }

            file.setName(newName);
            file.setIsDeleted(false);
            fileRepository.save(file);
        }

        // 하위 폴더 복구 재귀
        List<Folder> children = folderRepository.findByParentFolderId(folder.getId());
        for (Folder child : children) {
            restoreFolderAndContents(child);
        }
    }


    @Transactional
    public void deleteAllPermanently(List<Long> trashIds) {
        for (Long id : trashIds) {
            deletePermanently(id);
        }
    }

    public void deletePermanently(Long trashId) {
        TrashBin trash = trashBinRepository.findById(trashId)
                .orElseThrow(() -> new RuntimeException("휴지통 항목이 존재하지 않습니다."));

        if (trash.getFile() != null) {
            fileRepository.delete(trash.getFile());
        }

        if (trash.getFolder() != null) {
            deleteFolderAndContents(trash.getFolder());
        }

        trashBinRepository.delete(trash);
    }

    private void deleteFolderAndContents(Folder folder) {
        // 파일 삭제
        List<File> files = fileRepository.findByFolderId(folder.getId());
        fileRepository.deleteAll(files);

        // 하위 폴더 재귀 삭제
        List<Folder> children = folderRepository.findByParentFolderId(folder.getId());
        for (Folder child : children) {
            deleteFolderAndContents(child);
        }

        // folder_access 삭제
        folderAccessRepository.deleteAllByFolderId(folder.getId());

        folderRepository.delete(folder);
    }

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

    // 자동 삭제
    @Transactional
    public void deleteExpiredTrash() {
//        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(1); // 테스트용
        LocalDateTime expirationTime = LocalDateTime.now().minusDays(30);

        List<TrashBin> expired = trashBinRepository.findAll().stream()
                .filter(trash -> trash.getDeletedAt().toLocalDateTime().isBefore(expirationTime))
                .toList();

        List<Long> expiredTrashIds = expired.stream()
                .map(TrashBin::getId)
                .toList();

        deleteAllPermanently(expiredTrashIds);
    }
}
