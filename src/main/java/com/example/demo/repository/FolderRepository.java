package com.example.demo.repository;

import com.example.demo.entity.Folder;
import com.example.demo.entity.FolderType;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    // 1) 부모 폴더까지 포함하여 찾기 (부모가 null일 수도 있으므로 주의)
    Optional<Folder> findByNameAndParentFolderAndUser(String name, Folder parentFolder, User user);

    // 2) 부모가 null인 폴더를 찾기 위한 별도 메서드 (루트 폴더용)
    Optional<Folder> findByNameAndParentFolderIsNullAndUser(String name, User user);
    List<Folder> findByParentFolderId(Long parentFolderId);
    List<Folder> findByUserIdAndIsDeletedFalseAndFolderType(Long userId, FolderType folderType);
    @Transactional
    @Modifying
    @Query("DELETE FROM Folder f WHERE f.id = :id")
    void deleteByFolderId(@Param("id") @NonNull Long id);
    boolean existsByParentFolderIdAndNameAndIsDeletedFalse(Long parentFolderId, String name);
    boolean existsByParentFolderIdAndNameAndIdNotAndIsDeletedFalse(Long parentFolderId, String name, Long id);
    // 또는 parent가 null인 경우를 위해 별도로:
    boolean existsByUserIdAndParentFolderIsNullAndNameAndIsDeletedFalse(Long userId, String name);
    boolean existsByUserIdAndParentFolderIsNullAndName(Long userId, String name);

    @Query("SELECT f FROM Folder f WHERE f.id = :id AND f.isDeleted = false")
    Optional<Folder> findByIdAndIsDeletedFalse(@Param("id") Long id);
}
