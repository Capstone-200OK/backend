package com.example.demo.repository;

import com.example.demo.entity.ScheduledTask;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long> {

    /**
     * 현재 시간 이전 또는 같은 nextExecuted 값을 가진 예약 작업 조회
     *
     * @param now 기준 시간
     * @return 실행해야 할 예약 작업 리스트
     */
    List<ScheduledTask> findByNextExecutedLessThanEqual(LocalDateTime now);

    /**
     * 특정 사용자 ID로 예약 작업 전체 조회
     *
     * @param user 사용자 엔티티
     * @return 예약 작업 리스트
     */
    List<ScheduledTask> findAllByUser(User user);

    /**
     * 이전 폴더 ID로 예약 작업 전체 삭제
     *
     * @param previousFolderId 이전 폴더 ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ScheduledTask s WHERE s.previousFolder.id = :previousFolderId")
    void deleteAllByPreviousFolderId(@Param("previousFolderId") Long previousFolderId);

    /**
     * 새 폴더 ID로 예약 작업 전체 삭제
     *
     * @param newFolderId 새 폴더 ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ScheduledTask s WHERE s.newFolder.id = :newFolderId")
    void deleteAllByNewFolderId(@Param("newFolderId") Long newFolderId);
}
