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
    List<ScheduledTask> findByNextExecutedLessThanEqual(LocalDateTime now);

    List<ScheduledTask> findAllByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM ScheduledTask s WHERE s.previousFolder.id = :previousFolderId")
    void deleteAllByPreviousFolderId(@Param("previousFolderId") Long previousFolderId);
    @Modifying
    @Transactional
    @Query("DELETE FROM ScheduledTask s WHERE s.newFolder.id = :newFolderId")
    void deleteAllByNewFolderId(@Param("newFolderId") Long newFolderId);
}
