package com.example.demo.service;

import com.example.demo.dto.TrashBinRequestDTO;
import com.example.demo.dto.TrashBinResponseDTO;
import com.example.demo.entity.File;
import com.example.demo.entity.Folder;
import com.example.demo.entity.TrashBin;
import com.example.demo.entity.User;
import com.example.demo.repository.FileRepository;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.TrashBinRepository;
import com.example.demo.repository.UserRepository;
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

    @Transactional
    public TrashBin moveToTrash(TrashBinRequestDTO dto) {
        if ((dto.getFileId() == null && dto.getFolderId() == null) ||
                (dto.getFileId() != null && dto.getFolderId() != null)) {
            throw new IllegalArgumentException("fileId 또는 folderId 중 하나만 지정해야 합니다.");
        }

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        if (dto.getFileId() != null) {
            File file = fileRepository.findById(dto.getFileId())
                    .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

            // 상위 폴더가 삭제된 상태라면 직접 삭제하지 않음
            Folder parent = file.getFolder();
            if (parent != null && Boolean.TRUE.equals(parent.getIsDeleted())) {
                throw new IllegalStateException("상위 폴더가 삭제된 파일은 직접 삭제할 수 없습니다.");
            }

            if (Boolean.TRUE.equals(file.getIsDeleted())) {
                throw new IllegalStateException("이미 삭제된 파일입니다.");
            }

            file.setIsDeleted(true);

            TrashBin trash = TrashBin.builder()
                    .user(user)
                    .file(file)
                    .folder(file.getFolder())
                    .build();

            return trashBinRepository.save(trash);
        }

        if (dto.getFolderId() != null) {
            Folder folder = folderRepository.findById(dto.getFolderId())
                    .orElseThrow(() -> new RuntimeException("폴더를 찾을 수 없습니다."));

            if (folder.getParentFolder() != null && Boolean.TRUE.equals(folder.getParentFolder().getIsDeleted())) {
                throw new IllegalStateException("상위 폴더가 삭제된 폴더는 직접 삭제할 수 없습니다.");
            }

            if (Boolean.TRUE.equals(folder.getIsDeleted())) {
                throw new IllegalStateException("이미 삭제된 폴더입니다.");
            }

            // 재귀적으로 isDeleted 처리
            markFolderAndContentsAsDeleted(folder);

            TrashBin trash = TrashBin.builder()
                    .user(user)
                    .folder(folder)
                    .build();

            return trashBinRepository.save(trash);
        }

        throw new IllegalStateException("휴지통 이동 실패");
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
    public void restore(Long trashId) {
        TrashBin trash = trashBinRepository.findById(trashId)
                .orElseThrow(() -> new RuntimeException("휴지통 항목이 존재하지 않습니다."));

        if (trash.getFile() != null) {
            File file = trash.getFile();

            Folder parent = file.getFolder();
            if (parent != null && Boolean.TRUE.equals(parent.getIsDeleted())) {
                throw new RuntimeException("상위 폴더가 삭제된 상태에서는 복구할 수 없습니다.");
            }

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

        // 파일 복구
        List<File> files = fileRepository.findByFolderId(folder.getId());
        for (File file : files) {
            file.setIsDeleted(false);
        }

        // 하위 폴더 복구 재귀
        List<Folder> children = folderRepository.findByParentFolderId(folder.getId());
        for (Folder child : children) {
            restoreFolderAndContents(child);
        }
    }

    @Transactional
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
        for (File file : files) {
            fileRepository.delete(file);
        }

        // 하위 폴더 재귀 삭제
        List<Folder> children = folderRepository.findByParentFolderId(folder.getId());
        for (Folder child : children) {
            deleteFolderAndContents(child);
        }

        folderRepository.delete(folder);
    }

    @Transactional
    public List<TrashBinResponseDTO> getTrashFilesByUser(Long userId) {
        return trashBinRepository.findAll().stream()
                .filter(trash -> trash.getUser().getId().equals(userId))
                .map(trash -> TrashBinResponseDTO.builder()
                        .trashId(trash.getId())
                        .fileId(trash.getFile() != null ? trash.getFile().getId() : null)
                        .fileName(trash.getFile() != null ? trash.getFile().getName() : null)
                        .fileType(trash.getFile() != null ? trash.getFile().getFileType() : null)
                        .size(trash.getFile() != null ? trash.getFile().getSize() : null)
                        .folderId(trash.getFolder() != null ? trash.getFolder().getId() : null)
                        .folderName(trash.getFolder() != null ? trash.getFolder().getName() : null)
                        .deletedAt(trash.getDeletedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
