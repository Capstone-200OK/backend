package com.example.demo.repository;

import com.example.demo.entity.Folder;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    // 1) 부모 폴더까지 포함하여 찾기 (부모가 null일 수도 있으므로 주의)
    Optional<Folder> findByNameAndParentFolderAndUser(String name, Folder parentFolder, User user);

    // 2) 부모가 null인 폴더를 찾기 위한 별도 메서드 (루트 폴더용)
    Optional<Folder> findByNameAndParentFolderIsNullAndUser(String name, User user);
}
