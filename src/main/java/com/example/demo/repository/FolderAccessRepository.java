package com.example.demo.repository;

import com.example.demo.entity.Folder;
import com.example.demo.entity.FolderAccess;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderAccessRepository extends JpaRepository<FolderAccess, Long> {
    List<FolderAccess> findByUser(User user);
    boolean existsByUserAndFolder(User user, Folder folder);
    Optional<FolderAccess> findByUserIdAndFolderId(Long userId, Long folderId);

    List<FolderAccess> findByFolderId(Long parentId);
    @Modifying
    @Transactional
    @Query("DELETE FROM FolderAccess fa WHERE fa.folder.id = :folderId")
    void deleteAllByFolderId(Long folderId);
    List<FolderAccess> findAllByFolder(Folder folder);
}