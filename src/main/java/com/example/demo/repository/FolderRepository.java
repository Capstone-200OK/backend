package com.example.demo.repository;

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
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    /**
     * 사용자, 부모 폴더, 폴더 이름으로 폴더 조회
     *
     * @param name 폴더 이름
     * @param parentFolder 부모 폴더 (null 가능)
     * @param user 사용자 엔티티
     * @return 폴더 엔티티
     */
    Optional<Folder> findByNameAndParentFolderAndUser(String name, Folder parentFolder, User user);

    /**
     * 사용자, 폴더 이름으로 부모가 null인 폴더 조회 (루트 폴더)
     *
     * @param name 폴더 이름
     * @param user 사용자 엔티티
     * @return 폴더 엔티티
     */
    Optional<Folder> findByNameAndParentFolderIsNullAndUser(String name, User user);

    /**
     * 부모 폴더 ID로 하위 폴더 목록 조회
     *
     * @param parentFolderId 부모 폴더 ID
     * @return 하위 폴더 리스트
     */
    List<Folder> findByParentFolderId(Long parentFolderId);

    /**
     * 폴더 ID로 폴더 삭제
     *
     * @param id 삭제할 폴더 ID
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM Folder f WHERE f.id = :id")
    void deleteByFolderId(@Param("id") @NonNull Long id);

    /**
     * 동일 부모 폴더 내 동일 이름 폴더 존재 여부 확인
     *
     * @param parentFolderId 부모 폴더 ID
     * @param name 폴더 이름
     * @return 존재 여부
     */
    boolean existsByUserIdAndParentFolderIdAndNameAndIsDeletedFalse(Long userId, Long parentFolderId, String name);

    /**
     * 동일 부모 폴더 내 동일 이름 + 다른 ID 폴더 존재 여부 확인 (이름 중복 체크용)
     *
     * @param parentFolderId 부모 폴더 ID
     * @param name 폴더 이름
     * @param id 제외할 폴더 ID
     * @return 존재 여부
     */
    boolean existsByParentFolderIdAndNameAndIdNotAndIsDeletedFalse(Long parentFolderId, String name, Long id);

    /**
     * 사용자 루트 폴더 내 동일 이름 폴더 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @param name 폴더 이름
     * @return 존재 여부
     */
    boolean existsByUserIdAndParentFolderIsNullAndNameAndIsDeletedFalse(Long userId, String name);

    /**
     * 삭제되지 않은 폴더 ID로 폴더 조회
     *
     * @param id 폴더 ID
     * @return 폴더 엔티티
     */
    @Query("SELECT f FROM Folder f WHERE f.id = :id AND f.isDeleted = false")
    Optional<Folder> findByIdAndIsDeletedFalse(@Param("id") Long id);

    /**
     * 부모 폴더 ID로 하위 폴더 존재 여부 확인
     *
     * @param parentFolderId 부모 폴더 ID
     * @return 존재 여부
     */
    boolean existsByParentFolderIdAndIsDeletedFalse(Long parentFolderId);

    /**
     * 삭제되지 않은 모든 폴더 조회
     *
     * @return 폴더 리스트
     */
    List<Folder> findAllByIsDeletedFalse();

    /**
     * 사용자 소유 폴더 중 이름에 특정 문자열이 포함되고 삭제되지 않은 폴더 조회
     *
     * @param name 검색할 폴더 이름
     * @param user 사용자 엔티티
     * @return 폴더 리스트
     */
    List<Folder> findAllByNameContainingIgnoreCaseAndIsDeletedFalseAndUser(String name, User user);

    /**
     * 다른 사용자 소유 외부 클라우드 폴더 검색
     *
     * @param name 검색할 폴더 이름
     * @param user 현재 사용자 (제외 대상)
     * @return 외부 클라우드 폴더 리스트
     */
    @Transactional
    @Modifying
    @Query("""
        SELECT f FROM Folder f
        WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :name, '%'))
          AND f.isDeleted = false
          AND f.user <> :user
          AND f.folderType = "CLOUD"
    """)
    List<Folder> searchExternalCloudFolders(
            @Param("name") String name,
            @Param("user") User user
    );
}
