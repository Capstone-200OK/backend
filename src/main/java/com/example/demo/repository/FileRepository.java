package com.example.demo.repository;

import com.example.demo.entity.File;
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

    /**
     * 특정 폴더 ID로 파일 목록 조회
     *
     * @param folderId 폴더 ID
     * @return 파일 리스트
     */
    List<File> findByFolderId(Long folderId);

    /**
     * 특정 폴더 ID 내에서 삭제되지 않은 파일 목록 조회
     *
     * @param folderId 폴더 ID
     * @return 삭제되지 않은 파일 리스트
     */
    List<File> findByFolderIdAndIsDeletedFalse(Long folderId);

    /**
     * 동일 폴더 내 동일 이름의 파일 존재 여부 확인 (삭제되지 않은 파일)
     *
     * @param folderId 폴더 ID
     * @param name 파일 이름
     * @return 존재 여부
     */
    boolean existsByFolderIdAndNameAndIsDeletedFalse(Long folderId, String name);

    /**
     * 동일 폴더 내 동일 이름 + 다른 ID 파일 존재 여부 확인 (파일 이름 중복 체크용)
     *
     * @param folderId 폴더 ID
     * @param name 파일 이름
     * @param id 제외할 파일 ID
     * @return 존재 여부
     */
    boolean existsByFolderIdAndNameAndIdNotAndIsDeletedFalse(Long folderId, String name, Long id);

    /**
     * 특정 폴더 내 삭제되지 않은 파일 개수 조회
     *
     * @param folderId 폴더 ID
     * @return 파일 개수
     */
    long countByFolderIdAndIsDeletedFalse(Long folderId);

    /**
     * 특정 폴더에 삭제되지 않은 파일이 존재하는지 확인
     *
     * @param folderId 폴더 ID
     * @return 존재 여부
     */
    boolean existsByFolderIdAndIsDeletedFalse(Long folderId);

    /**
     * 특정 폴더에서 사용자 ID와 파일 이름으로 중복 파일 존재 여부 확인
     *
     * @param folderId 폴더 ID
     * @param userId 사용자 ID
     * @param newName 파일 이름
     * @return 존재 여부
     */
    boolean existsByFolderIdAndUserIdAndNameAndIsDeletedFalse(Long folderId, Long userId, String newName);

    /**
     * 파일 ID로 파일 삭제
     *
     * @param id 삭제할 파일 ID
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM File f WHERE f.id = :id")
    void deleteByFileId(@Param("id") @NonNull Long id);

    /**
     * 이름에 특정 문자열이 포함되고 삭제되지 않은 파일 목록 조회 (대소문자 무시)
     *
     * @param name 검색할 이름
     * @param user 사용자 엔티티
     * @return 파일 리스트
     */
    List<File> findAllByNameContainingIgnoreCaseAndIsDeletedFalseAndUser(String name, User user);
}
