package com.example.demo.dto;

import com.example.demo.entity.ScheduleInterval;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledTaskDTO {
    private Long userId;
    private Long folderId;
    private ScheduleInterval interval;
}
