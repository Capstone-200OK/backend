package com.example.demo.repository;

import com.example.demo.entity.File;
import com.example.demo.entity.FileSortingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FileSortingHistoryRepository extends JpaRepository<FileSortingHistory, Long> {
    List<FileSortingHistory> findBySortingId(Long sortingId);

    @Modifying
    @Transactional
    @Query("DELETE FROM FileSortingHistory f WHERE f.sorting.id = :sortingId")
    void deleteAllBySortingId(@Param("sortingId") Long sortingId);
    void deleteAllByFileIn(List<File> files);
    @Modifying
    @Transactional
    @Query("DELETE FROM FileSortingHistory f WHERE f.previousFolder.id = :previousFolderId")
    void deleteAllByPreviousFolderId(@Param("previousFolderId") Long previousFolderId);
    @Modifying
    @Transactional
    @Query("DELETE FROM FileSortingHistory f WHERE f.newFolder.id = :newFolderId")
    void deleteAllByNewFolderId(@Param("newFolderId") Long newFolderId);
}
