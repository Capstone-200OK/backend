package com.example.demo.repository;

import com.example.demo.entity.Folder;
import com.example.demo.entity.FolderAccess;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderAccessRepository extends JpaRepository<FolderAccess, Long> {

    /**
     * 사용자로 폴더 접근 권한 목록 조회
     *
     * @param user 사용자 엔티티
     * @return 폴더 접근 권한 리스트
     */
    List<FolderAccess> findByUser(User user);

    /**
     * 사용자 ID와 폴더 ID로 접근 권한 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @param folderId 폴더 ID
     * @return 존재 여부
     */
    boolean existsByUserIdAndFolderId(Long userId, Long folderId);

    /**
     * 사용자 ID와 폴더 ID로 접근 권한 조회
     *
     * @param userId 사용자 ID
     * @param folderId 폴더 ID
     * @return 폴더 접근 권한
     */
    Optional<FolderAccess> findByUserIdAndFolderId(Long userId, Long folderId);

    /**
     * 폴더 ID로 접근 권한 리스트 조회
     *
     * @param parentId 폴더 ID
     * @return 폴더 접근 권한 리스트
     */
    List<FolderAccess> findByFolderId(Long parentId);

    /**
     * 폴더 ID로 접근 권한 전체 삭제
     *
     * @param folderId 삭제할 폴더 ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM FolderAccess fa WHERE fa.folder.id = :folderId")
    void deleteAllByFolderId(Long folderId);

    /**
     * 폴더 엔티티로 접근 권한 전체 조회
     *
     * @param folder 폴더 엔티티
     * @return 폴더 접근 권한 리스트
     */
    List<FolderAccess> findAllByFolder(Folder folder);
}
