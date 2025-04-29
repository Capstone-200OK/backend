package com.example.demo.repository;

import com.example.demo.entity.ScheduledTask;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long> {
    List<ScheduledTask> findByNextExecutedLessThanEqual(LocalDateTime now);

    List<ScheduledTask> findAllByUser(User user);
}
