package com.example.demo.dto.scheduledTaskDTO;

import com.example.demo.entity.Criteria;
import com.example.demo.entity.ScheduleInterval;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScheduledTaskResponseDTO {
    private Long taskId;
    private Long userId;
    private Long previousFolderId;
    private Long newFolderId;
    private Criteria criteria;
    private ScheduleInterval interval;
    private LocalDateTime nextExecuted;
}
