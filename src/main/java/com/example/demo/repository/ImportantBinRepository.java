package com.example.demo.repository;

import com.example.demo.entity.File;
import com.example.demo.entity.Folder;
import com.example.demo.entity.ImportantBin;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ImportantBinRepository extends JpaRepository<ImportantBin, Long> {

    /**
     * 특정 폴더 ID로 중요 문서함 항목 삭제
     *
     * @param folderId 삭제할 폴더 ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ImportantBin ib WHERE ib.folder.id = :folderId")
    void deleteAllByFolderId(Long folderId);

    /**
     * 특정 파일 ID로 중요 문서함 항목 삭제
     *
     * @param fileId 삭제할 파일 ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ImportantBin ib WHERE ib.file.id = :fileId")
    void deleteAllByFileId(Long fileId);

    /**
     * 사용자와 파일로 중복 여부 확인
     *
     * @param user 사용자 엔티티
     * @param file 파일 엔티티
     * @return 이미 추가되어 있으면 true
     */
    boolean existsByUserAndFile(User user, File file);

    /**
     * 사용자와 폴더로 중복 여부 확인
     *
     * @param user 사용자 엔티티
     * @param folder 폴더 엔티티
     * @return 이미 추가되어 있으면 true
     */
    boolean existsByUserAndFolder(User user, Folder folder);

    /**
     * 파일 리스트로 중요 문서함 항목 삭제
     *
     * @param files 삭제할 파일 리스트
     */
    void deleteAllByFileIn(List<File> files);
}
