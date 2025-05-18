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

    /**
     * 사용자가 접근 가능한 클라우드 폴더 목록 조회
     *
     * @param user 사용자 객체
     * @return 접근 가능한 클라우드 폴더 리스트
     */
    public List<Folder> getAccessibleCloudFolders(User user) {
        return folderAccessRepository.findByUser(user).stream()
                .map(FolderAccess::getFolder)
                .filter(folder -> folder.getFolderType() == FolderType.CLOUD && !folder.getIsDeleted())
                .toList();
    }

    /**
     * 사용자가 해당 폴더에 읽기 권한이 있는지 확인
     *
     * @param user 사용자
     * @param folder 폴더
     * @return 읽기 권한 여부
     */
    public boolean canRead(User user, Folder folder) {
        if (folder.getFolderType() == FolderType.PERSONAL) {
            return folder.getUser().equals(user);
        }
        return getAccess(user, folder)
                .map(access -> FolderPermissionUtil.canRead(access.getChmod()))
                .orElse(false);
    }

    /**
     * 사용자가 해당 폴더에 쓰기 권한이 있는지 확인
     *
     * @param user 사용자
     * @param folder 폴더
     * @return 쓰기 권한 여부
     */
    public boolean canWrite(User user, Folder folder) {
        if (folder.getFolderType() == FolderType.PERSONAL) {
            return folder.getUser().equals(user);
        }
        return getAccess(user, folder)
                .map(access -> FolderPermissionUtil.canWrite(access.getChmod()))
                .orElse(false);
    }

    /**
     * 사용자가 해당 폴더에 삭제 권한이 있는지 확인
     *
     * @param user 사용자
     * @param folder 폴더
     * @return 삭제 권한 여부
     */
    public boolean canDelete(User user, Folder folder) {
        if (folder.getFolderType() == FolderType.PERSONAL) {
            return folder.getUser().equals(user);
        }
        return getAccess(user, folder)
                .map(access -> FolderPermissionUtil.canDelete(access.getChmod()))
                .orElse(false);
    }

    /**
     * 사용자가 해당 폴더에 어떤 형태로든 접근 권한이 있는지 확인
     *
     * @param user 사용자
     * @param folder 폴더
     * @return 접근 권한 여부
     */
    public boolean canAccess(User user, Folder folder) {
        return canRead(user, folder) || canWrite(user, folder) || canDelete(user, folder);
    }

    /**
     * 폴더에 대한 접근 권한 부여 또는 갱신
     *
     * @param userId 사용자 ID
     * @param folderId 폴더 ID
     * @param chmod 권한 (읽기/쓰기/삭제 등)
     */
    public void grantAccess(Long userId, Long folderId, int chmod) {
        Optional<FolderAccess> existing = folderAccessRepository.findByUserIdAndFolderId(userId, folderId);
        if (existing.isPresent()) {
            // 기존 권한이 존재하면 수정
            FolderAccess access = existing.get();
            access.setChmod(chmod);
            folderAccessRepository.save(access);
        } else {
            // 새로 권한 생성
            FolderAccess access = FolderAccess.builder()
                    .user(User.builder().id(userId).build())
                    .folder(Folder.builder().id(folderId).build())
                    .chmod(chmod)
                    .build();
            folderAccessRepository.save(access);
        }
    }

    /**
     * 사용자의 특정 폴더에 대한 FolderAccess 조회
     *
     * @param user 사용자
     * @param folder 폴더
     * @return FolderAccess 객체(Optional)
     */
    private Optional<FolderAccess> getAccess(User user, Folder folder) {
        return folderAccessRepository.findByUserIdAndFolderId(user.getId(), folder.getId());
    }

    /**
     * 폴더 ID 기준으로 모든 접근 권한 리스트 반환
     *
     * @param parentId 폴더 ID
     * @return 접근 권한 리스트
     */
    public List<FolderAccess> getAllAccessByFolder(Long parentId) {
        return folderAccessRepository.findByFolderId(parentId);
    }

    /**
     * 사용자가 접근 가능한 루트 클라우드 폴더 조회 (이름 기준)
     *
     * @param user 사용자
     * @param name 폴더 이름
     * @return 해당 이름의 루트 클라우드 폴더(Optional)
     */
    @Transactional(readOnly = true)
    public Optional<Folder> findAccessibleRootCloudFolderByName(User user, String name) {
        return folderAccessRepository.findByUser(user).stream()
                .map(FolderAccess::getFolder)
                .filter(folder -> folder.getParentFolder() == null) // 루트 폴더인지 확인
                .filter(folder -> folder.getFolderType() == FolderType.CLOUD)
                .filter(folder -> !folder.getIsDeleted())
                .filter(folder -> folder.getName().equals(name))
                .findFirst();
    }

    /**
     * 사용자가 접근 가능한 하위 클라우드 폴더 조회 (이름 기준)
     *
     * @param user 사용자
     * @param parentFolder 부모 폴더
     * @param name 폴더 이름
     * @return 해당 이름의 하위 클라우드 폴더(Optional)
     */
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

    /**
     * 해당 사용자가 폴더에 모든 권한(읽기, 쓰기, 삭제)을 갖고 있는지 확인
     *
     * @param user 사용자
     * @param folder 폴더
     * @return 전체 권한 여부
     */
    public boolean hasFullPermission(User user, Folder folder) {
        return folderAccessRepository.findByUserIdAndFolderId(user.getId(), folder.getId())
                .map(access -> access.getChmod() == 7)
                .orElse(false);
    }
}
