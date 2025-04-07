package com.example.demo.controller;

import com.example.demo.dto.ScheduledTaskDTO;
import com.example.demo.entity.ScheduledTask;
import com.example.demo.service.ScheduledTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController  // Controller → RestController 변경 (JSON 반환)
@RequiredArgsConstructor
@RequestMapping("/scheduledTask")
public class ScheduledTaskController {
    private final ScheduledTaskService scheduledTaskService;

    // 예약 작업 추가
    @PostMapping("/add")
    public ResponseEntity<ScheduledTask> add(@RequestBody ScheduledTaskDTO scheduledTaskDTO) {
        return ResponseEntity.ok(scheduledTaskService.addScheduledTask(scheduledTaskDTO));
    }

    // 예약 작업 삭제
    @PostMapping("/delete")
    public ResponseEntity<Boolean> delete(@RequestParam Long taskId) {
        scheduledTaskService.deleteScheduledTask(taskId);
        return ResponseEntity.ok(true);
    }

    // 예약 작업 수정
    @PostMapping("/modify")
    public ResponseEntity<ScheduledTask> modify(@RequestParam Long taskId, @RequestBody ScheduledTaskDTO scheduledTaskDTO) {
        return ResponseEntity.ok(scheduledTaskService.modifyScheduledTask(taskId, scheduledTaskDTO));
    }
}
