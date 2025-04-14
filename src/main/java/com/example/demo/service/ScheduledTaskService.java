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

import java.sql.Timestamp;

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

        Folder previousFolder = folderRepository.findById(scheduledTaskDTO.getPreviousFolderId())
                .orElseThrow(() -> new IllegalArgumentException("Previous folder not found"));

        Folder newFolder = folderRepository.findById(scheduledTaskDTO.getNewFolderId())
                .orElseThrow(() -> new IllegalArgumentException("New folder not found"));

        ScheduledTask scheduledTask = ScheduledTask.builder()
                .user(user)
                .previousFolder(previousFolder)
                .newFolder(newFolder)
                .criteria(scheduledTaskDTO.getCriteria())
                .interval(scheduledTaskDTO.getInterval())
                .nextExecuted(scheduledTaskDTO.getNextExecuted())
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

        if (scheduledTaskDTO.getCriteria() != null) {
            scheduledTask.setCriteria(scheduledTaskDTO.getCriteria());
        }

        if (scheduledTaskDTO.getPreviousFolderId() != null) {
            Folder previousFolder = folderRepository.findById(scheduledTaskDTO.getPreviousFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("Previous folder not found"));
            scheduledTask.setPreviousFolder(previousFolder);
        }

        if (scheduledTaskDTO.getNewFolderId() != null) {
            Folder newFolder = folderRepository.findById(scheduledTaskDTO.getNewFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("New folder not found"));
            scheduledTask.setNewFolder(newFolder);
        }

        if (scheduledTaskDTO.getNextExecuted() != null) {
            scheduledTask.setNextExecuted(scheduledTaskDTO.getNextExecuted());
        }

        return scheduledTaskRepository.save(scheduledTask);
    }
}
