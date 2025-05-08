package com.example.demo.repository;

import com.example.demo.entity.File;
import com.example.demo.entity.Folder;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByFolderId(Long folderId);
    // 부모 폴더 안에서 같은 이름+확장자의 파일이 존재하는지 확인
    List<File> findByFolderIdAndIsDeletedFalse(Long folderId);
    boolean existsByFolderIdAndNameAndIsDeletedFalse(Long folderId, String name);
    boolean existsByFolderIdAndNameAndIdNotAndIsDeletedFalse(Long folderId, String name, Long id);
    long countByFolderIdAndIsDeletedFalse(Long folderId);
    boolean existsByFolderIdAndIsDeletedFalse(Long folderId);
    boolean existsByFolderIdAndUserIdAndNameAndIsDeletedFalse(Long folderId, Long userId, String newName);
    @Transactional
    @Modifying
    @Query("DELETE FROM File f WHERE f.id = :id")
    void deleteByFileId(@Param("id") @NonNull Long id);
    List<File> findAllByNameContainingIgnoreCaseAndIsDeletedFalseAndUser(String name, User user);
}
