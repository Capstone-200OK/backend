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

    /**
     * 정리 기록 ID로 파일 정리 이력 조회
     *
     * @param sortingId 정리 기록 ID
     * @return 파일 정리 이력 리스트
     */
    List<FileSortingHistory> findBySortingId(Long sortingId);

    /**
     * 정리 기록 ID로 파일 정리 이력 삭제
     *
     * @param sortingId 삭제할 정리 기록 ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM FileSortingHistory f WHERE f.sorting.id = :sortingId")
    void deleteAllBySortingId(@Param("sortingId") Long sortingId);

    /**
     * 파일 리스트로 파일 정리 이력 삭제
     *
     * @param files 삭제할 파일 리스트
     */
    void deleteAllByFileIn(List<File> files);

    /**
     * 이전 폴더 ID로 파일 정리 이력 삭제
     *
     * @param previousFolderId 이전 폴더 ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM FileSortingHistory f WHERE f.previousFolder.id = :previousFolderId")
    void deleteAllByPreviousFolderId(@Param("previousFolderId") Long previousFolderId);

    /**
     * 새 폴더 ID로 파일 정리 이력 삭제
     *
     * @param newFolderId 새 폴더 ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM FileSortingHistory f WHERE f.newFolder.id = :newFolderId")
    void deleteAllByNewFolderId(@Param("newFolderId") Long newFolderId);
}
