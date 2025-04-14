package com.example.demo.dto;

import com.example.demo.entity.ScheduleInterval;
import com.example.demo.entity.Criteria;
import java.time.LocalDateTime;

import lombok.*;

@Data
public class ScheduledTaskDTO {
    private Long userId;
    private Long previousFolderId;
    private Long newFolderId;
    private Criteria criteria;
    private ScheduleInterval interval;
    private LocalDateTime nextExecuted;
}

