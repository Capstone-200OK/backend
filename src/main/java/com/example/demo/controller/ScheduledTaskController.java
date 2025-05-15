package com.example.demo.controller;

import com.example.demo.dto.MessageResponse;
import com.example.demo.dto.scheduledTaskDTO.ScheduledTaskRequestDTO;
import com.example.demo.dto.scheduledTaskDTO.ScheduledTaskResponseDTO;
import com.example.demo.service.ScheduledTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/scheduledTask")
public class ScheduledTaskController {
    private final ScheduledTaskService scheduledTaskService;

    // 예약 작업 추가
    @PostMapping("/add")
    public ResponseEntity<MessageResponse> add(@RequestBody ScheduledTaskRequestDTO scheduledTaskRequestDTO) {
        scheduledTaskService.addScheduledTask(scheduledTaskRequestDTO);
        return ResponseEntity.ok(new MessageResponse("Scheduled task has been added"));
    }

    // 예약 작업 삭제
    @DeleteMapping("/delete/{taskId}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long taskId) {
        scheduledTaskService.deleteScheduledTask(taskId);
        return ResponseEntity.ok(new MessageResponse("Scheduled task has been deleted"));
    }

    // 예약 작업 수정
    @PostMapping("/modify/{taskId}")
    public ResponseEntity<MessageResponse> modify(@PathVariable Long taskId, @RequestBody ScheduledTaskRequestDTO scheduledTaskRequestDTO) {
        scheduledTaskService.modifyScheduledTask(taskId, scheduledTaskRequestDTO);
        return ResponseEntity.ok(new MessageResponse("Scheduled task has been updated"));
    }

    // 예약 작업 리스트 반환
    @GetMapping("/list/{userId}")
    public ResponseEntity<List<ScheduledTaskResponseDTO>> getList(@PathVariable Long userId) {
        List<ScheduledTaskResponseDTO> taskList = scheduledTaskService.getScheduledTasksByUser(userId);
        return ResponseEntity.ok(taskList);
    }
}
