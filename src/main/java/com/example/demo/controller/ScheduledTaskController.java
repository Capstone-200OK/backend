package com.example.demo.controller;

import com.example.demo.dto.scheduledTaskDTO.ScheduledTaskDTO;
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
    public ResponseEntity<Void> add(@RequestBody ScheduledTaskDTO scheduledTaskDTO) {
        scheduledTaskService.addScheduledTask(scheduledTaskDTO);
        return ResponseEntity.ok().build();
    }

    // 예약 작업 삭제
    @DeleteMapping("/delete/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable Long taskId) {
        scheduledTaskService.deleteScheduledTask(taskId);
        return ResponseEntity.ok().build();
    }

    // 예약 작업 수정
    @PostMapping("/modify/{taskId}")
    public ResponseEntity<Void> modify(@PathVariable Long taskId, @RequestBody ScheduledTaskDTO scheduledTaskDTO) {
        scheduledTaskService.modifyScheduledTask(taskId, scheduledTaskDTO);
        return ResponseEntity.ok().build();
    }
}
