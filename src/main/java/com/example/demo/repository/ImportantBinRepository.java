package com.example.demo.repository;

import com.example.demo.entity.ImportantBin;
import com.example.demo.entity.User;
import com.example.demo.entity.File;
import com.example.demo.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportantBinRepository extends JpaRepository<ImportantBin, Long> {
    List<ImportantBin> findAllByUser(User user);
    @Modifying
    @Transactional
    @Query("DELETE FROM ImportantBin ib WHERE ib.folder.id = :folderId")
    void deleteAllByFolderId(Long folderId);
    @Modifying
    @Transactional
    @Query("DELETE FROM ImportantBin ib WHERE ib.file.id = :fileId")
    void deleteAllByFileId(Long fileId);
    boolean existsByUserAndFile(User user, File file);
    boolean existsByUserAndFolder(User user, Folder folder);
}
