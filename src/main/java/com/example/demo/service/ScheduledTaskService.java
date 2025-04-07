package com.example.demo.service;

import com.example.demo.dto.ScheduledTaskDTO;
import com.example.demo.entity.Folder;
import com.example.demo.entity.ScheduledTask;
import com.example.demo.entity.User;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.ScheduledTaskRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduledTaskService {
    private final ScheduledTaskRepository scheduledTaskRepository;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;

    @Transactional
    public ScheduledTask addScheduledTask(ScheduledTaskDTO scheduledTaskDTO) {
        User user = userRepository.findById(scheduledTaskDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Folder folder = folderRepository.findById(scheduledTaskDTO.getFolderId())
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        ScheduledTask scheduledTask = ScheduledTask.builder()
                .user(user)
                .folder(folder)
                .interval(scheduledTaskDTO.getInterval())
                .build();

        return scheduledTaskRepository.save(scheduledTask);
    }

    @Transactional
    public void deleteScheduledTask(Long taskId) {
        if (!scheduledTaskRepository.existsById(taskId)) {
            throw new IllegalArgumentException("Task not found");
        }
        scheduledTaskRepository.deleteById(taskId);
    }

    @Transactional
    public ScheduledTask modifyScheduledTask(Long taskId, ScheduledTaskDTO scheduledTaskDTO) {
        ScheduledTask scheduledTask = scheduledTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (scheduledTaskDTO.getInterval() != null) {
            scheduledTask.setInterval(scheduledTaskDTO.getInterval());
        }

        return scheduledTaskRepository.save(scheduledTask);
    }
}
