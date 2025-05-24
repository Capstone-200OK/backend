package com.example.demo.dto.scheduledTaskDTO;

import com.example.demo.entity.Criteria;
import com.example.demo.entity.ScheduleInterval;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduledTaskRequestDTO {
    private Long userId;
    private Long previousFolderId;
    private Long newFolderId;
    private Criteria criteria;
    private ScheduleInterval interval;
    private LocalDateTime nextExecuted;
    private Boolean isMaintain;
    private Boolean fileNameChange;
}

