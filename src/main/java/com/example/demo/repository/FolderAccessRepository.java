package com.example.demo.repository;

import com.example.demo.entity.Folder;
import com.example.demo.entity.FolderAccess;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderAccessRepository extends JpaRepository<FolderAccess, Long> {
    List<FolderAccess> findByUser(User user);
    boolean existsByUserAndFolder(User user, Folder folder);
    Optional<FolderAccess> findByUserIdAndFolderId(Long userId, Long folderId);

    List<FolderAccess> findByFolderId(Long parentId);
}