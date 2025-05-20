package com.example.demo.repository;

import com.example.demo.entity.FolderSortingHistory;
import com.example.demo.entity.FolderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FolderSortingHistoryRepository extends JpaRepository<FolderSortingHistory, Long> {

    /**
     * 정리 기록 ID와 폴더 상태로 폴더 정리 이력 조회
     *
     * @param sortingId 정리 기록 ID
     * @param status 폴더 상태 (CREATED, DELETED 등)
     * @return 해당 조건에 맞는 폴더 정리 이력 리스트
     */
    List<FolderSortingHistory> findBySortingIdAndStatus(Long sortingId, FolderStatus status);

    /**
     * 정리 기록 ID로 폴더 정리 이력 전체 삭제
     *
     * @param sortingId 삭제할 정리 기록 ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM FolderSortingHistory f WHERE f.sorting.id = :sortingId")
    void deleteAllBySortingId(@Param("sortingId") Long sortingId);

    /**
     * 폴더 ID로 폴더 정리 이력 전체 삭제
     *
     * @param folderId 삭제할 폴더 ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM FolderSortingHistory fsh WHERE fsh.folder.id = :folderId")
    void deleteAllByFolderId(Long folderId);
}
