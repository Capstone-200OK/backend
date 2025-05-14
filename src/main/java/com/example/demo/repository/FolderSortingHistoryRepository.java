package com.example.demo.repository;

import com.example.demo.entity.FolderSortingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.FolderStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FolderSortingHistoryRepository extends JpaRepository<FolderSortingHistory, Long> {
    // 정리 ID와 상태로 정리 기록 찾기
    List<FolderSortingHistory> findBySortingIdAndStatus(Long sortingId, FolderStatus status);

    // 전체 폴더 정리 기록 가져오기
    List<FolderSortingHistory> findBySortingId(Long sortingId);

    @Modifying
    @Transactional
    @Query("DELETE FROM FolderSortingHistory f WHERE f.sorting.id = :sortingId")
    void deleteAllBySortingId(@Param("sortingId") Long sortingId);
    @Modifying
    @Transactional
    @Query("DELETE FROM FolderSortingHistory fsh WHERE fsh.folder.id = :folderId")
    void deleteAllByFolderId(Long folderId);
}
