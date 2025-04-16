package com.example.demo.repository;

import com.example.demo.entity.ScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long> {
    List<ScheduledTask> findByNextExecutedBeforeOrNextExecutedEquals(LocalDateTime before, LocalDateTime equals);
}
