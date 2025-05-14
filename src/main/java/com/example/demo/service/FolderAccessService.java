package com.example.demo.service;

import com.example.demo.entity.Folder;
import com.example.demo.entity.FolderAccess;
import com.example.demo.entity.FolderType;
import com.example.demo.entity.User;
import com.example.demo.repository.FolderAccessRepository;
import com.example.demo.util.FolderPermissionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FolderAccessService {

    private final FolderAccessRepository folderAccessRepository;

    public List<Folder> getAccessibleCloudFolders(User user) {
        return folderAccessRepository.findByUser(user).stream()
                .map(FolderAccess::getFolder)
                .filter(folder -> folder.getFolderType() == FolderType.CLOUD && !folder.getIsDeleted())
                .toList();
    }

    public boolean canRead(User user, Folder folder) {
        if (folder.getFolderType() == FolderType.PERSONAL) {
            return folder.getUser().equals(user);
        }
        return getAccess(user, folder)
                .map(access -> FolderPermissionUtil.canRead(access.getChmod()))
                .orElse(false);
    }

    public boolean canWrite(User user, Folder folder) {
        if (folder.getFolderType() == FolderType.PERSONAL) {
            return folder.getUser().equals(user);
        }
        return getAccess(user, folder)
                .map(access -> FolderPermissionUtil.canWrite(access.getChmod()))
                .orElse(false);
    }

    public boolean canDelete(User user, Folder folder) {
        if (folder.getFolderType() == FolderType.PERSONAL) {
            return folder.getUser().equals(user);
        }
        return getAccess(user, folder)
                .map(access -> FolderPermissionUtil.canDelete(access.getChmod()))
                .orElse(false);
    }

    public boolean canAccess(User user, Folder folder) {
        return canRead(user, folder) || canWrite(user, folder) || canDelete(user, folder);
    }

    public void grantAccess(Long userId, Long folderId, int chmod) {
        Optional<FolderAccess> existing = folderAccessRepository.findByUserIdAndFolderId(userId, folderId);
        if (existing.isPresent()) {
            FolderAccess access = existing.get();
            access.setChmod(chmod);
            folderAccessRepository.save(access);
        } else {
            FolderAccess access = FolderAccess.builder()
                    .user(User.builder().id(userId).build())
                    .folder(Folder.builder().id(folderId).build())
                    .chmod(chmod)
                    .build();
            folderAccessRepository.save(access);
        }
    }

    private Optional<FolderAccess> getAccess(User user, Folder folder) {
        return folderAccessRepository.findByUserIdAndFolderId(user.getId(), folder.getId());
    }

    public List<FolderAccess> getAllAccessByFolder(Long parentId) {
        return folderAccessRepository.findByFolderId(parentId);
    }

    public Folder findTopAccessibleRoot(User user, Folder folder) {
        Folder current = folder;

        while (current.getParentFolder() != null) {
            Folder parent = current.getParentFolder();

            if (current.getFolderType() == FolderType.PERSONAL) {
                // 개인 폴더: 사용자가 소유자인지 확인
                if (!parent.getUser().getId().equals(user.getId())) break;
            } else if (current.getFolderType() == FolderType.CLOUD) {
                // 클라우드 폴더: 상위 폴더 접근 가능한지 확인
                if (!canAccess(user, parent)) break;
            }

            current = parent;
        }

        return current;
    }


    @Transactional(readOnly = true)
    public Optional<Folder> findAccessibleRootCloudFolderByName(User user, String name) {
        return folderAccessRepository.findByUser(user).stream()
                .map(FolderAccess::getFolder)
                .filter(folder -> folder.getParentFolder() == null)   // 루트 폴더
                .filter(folder -> folder.getFolderType() == FolderType.CLOUD)
                .filter(folder -> !folder.getIsDeleted())
                .filter(folder -> folder.getName().equals(name))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public Optional<Folder> findAccessibleSubCloudFolderByName(User user, Folder parentFolder, String name) {
        return folderAccessRepository.findByUser(user).stream()
                .map(FolderAccess::getFolder)
                .filter(folder -> parentFolder.equals(folder.getParentFolder()))
                .filter(folder -> folder.getFolderType() == FolderType.CLOUD)
                .filter(folder -> !folder.getIsDeleted())
                .filter(folder -> folder.getName().equals(name))
                .findFirst();
    }
    public boolean hasFullPermission(User user, Folder folder) {
        return folderAccessRepository.findByUserIdAndFolderId(user.getId(), folder.getId())
                .map(access -> access.getChmod() == 7)
                .orElse(false);
    }

}
