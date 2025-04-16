package com.example.demo.controller;

import com.example.demo.dto.scheduledTaskDTO.ScheduledTaskDTO;
import com.example.demo.service.ScheduledTaskService;
import com.example.demo.dto.MessageResponse;
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
    public ResponseEntity<MessageResponse> add(@RequestBody ScheduledTaskDTO scheduledTaskDTO) {
        scheduledTaskService.addScheduledTask(scheduledTaskDTO);
        return ResponseEntity.ok(new MessageResponse("예약 작업이 추가되었습니다."));
    }

    // 예약 작업 삭제
    @DeleteMapping("/delete/{taskId}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long taskId) {
        scheduledTaskService.deleteScheduledTask(taskId);
        return ResponseEntity.ok(new MessageResponse("예약 작업이 삭제되었습니다."));
    }

    // 예약 작업 수정
    @PostMapping("/modify/{taskId}")
    public ResponseEntity<MessageResponse> modify(@PathVariable Long taskId, @RequestBody ScheduledTaskDTO scheduledTaskDTO) {
        scheduledTaskService.modifyScheduledTask(taskId, scheduledTaskDTO);
        return ResponseEntity.ok(new MessageResponse("예약 작업이 수정되었습니다."));
    }
}
