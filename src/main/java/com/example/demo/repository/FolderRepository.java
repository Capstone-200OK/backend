package com.example.demo.repository;

import com.example.demo.entity.Folder;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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

    // 부모 폴더 안에서 같은 이름의 폴더가 존재하는지 확인
    boolean existsByUserIdAndParentFolderAndName(Long userId, Folder parentFolder, String name);

    @Transactional
    @Modifying
    @Query("DELETE FROM Folder f WHERE f.id IN :ids")
    void deleteAll(@Param("ids") List<Long> ids);
    boolean existsByUserIdAndParentFolderIdAndName(Long userId, Long parentFolderId, String name);

    // 또는 parent가 null인 경우를 위해 별도로:
    boolean existsByUserIdAndParentFolderIsNullAndName(Long userId, String name);

}
