package com.example.demo.service;

import com.example.demo.dto.scheduledTaskDTO.ScheduledTaskRequestDTO;
import com.example.demo.dto.scheduledTaskDTO.ScheduledTaskResponseDTO;
import com.example.demo.entity.Folder;
import com.example.demo.entity.ScheduledTask;
import com.example.demo.entity.User;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.ScheduledTaskRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduledTaskService {
    private final ScheduledTaskRepository scheduledTaskRepository;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;

    @Transactional
    public void addScheduledTask(ScheduledTaskRequestDTO scheduledTaskRequestDTO) {
        User user = userRepository.findById(scheduledTaskRequestDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Folder previousFolder = folderRepository.findById(scheduledTaskRequestDTO.getPreviousFolderId())
                .orElseThrow(() -> new IllegalArgumentException("Previous folder not found"));

        Folder newFolder = folderRepository.findById(scheduledTaskRequestDTO.getNewFolderId())
                .orElseThrow(() -> new IllegalArgumentException("New folder not found"));

        ScheduledTask scheduledTask = ScheduledTask.builder()
                .user(user)
                .previousFolder(previousFolder)
                .newFolder(newFolder)
                .criteria(scheduledTaskRequestDTO.getCriteria())
                .interval(scheduledTaskRequestDTO.getInterval())
                .nextExecuted(scheduledTaskRequestDTO.getNextExecuted())
                .build();

        scheduledTaskRepository.save(scheduledTask);
    }

    @Transactional
    public void deleteScheduledTask(Long taskId) {
        if (!scheduledTaskRepository.existsById(taskId)) {
            throw new IllegalArgumentException("Task not found");
        }
        scheduledTaskRepository.deleteById(taskId);
    }

    @Transactional
    public void modifyScheduledTask(Long taskId, ScheduledTaskRequestDTO scheduledTaskRequestDTO) {
        ScheduledTask scheduledTask = scheduledTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (scheduledTaskRequestDTO.getInterval() != null) {
            scheduledTask.setInterval(scheduledTaskRequestDTO.getInterval());
        }

        if (scheduledTaskRequestDTO.getCriteria() != null) {
            scheduledTask.setCriteria(scheduledTaskRequestDTO.getCriteria());
        }

        if (scheduledTaskRequestDTO.getPreviousFolderId() != null) {
            Folder previousFolder = folderRepository.findById(scheduledTaskRequestDTO.getPreviousFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("Previous folder not found"));
            scheduledTask.setPreviousFolder(previousFolder);
        }

        if (scheduledTaskRequestDTO.getNewFolderId() != null) {
            Folder newFolder = folderRepository.findById(scheduledTaskRequestDTO.getNewFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("New folder not found"));
            scheduledTask.setNewFolder(newFolder);
        }

        if (scheduledTaskRequestDTO.getNextExecuted() != null) {
            scheduledTask.setNextExecuted(scheduledTaskRequestDTO.getNextExecuted());
        }

        scheduledTaskRepository.save(scheduledTask);
    }

    @Transactional(readOnly = true)
    public List<ScheduledTaskResponseDTO> getScheduledTasksByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<ScheduledTask> tasks = scheduledTaskRepository.findAllByUser(user);

        return tasks.stream()
                .map(task -> ScheduledTaskResponseDTO.builder()
                        .taskId(task.getId())
                        .userId(task.getUser().getId())
                        .previousFolderId(task.getPreviousFolder().getId())
                        .newFolderId(task.getNewFolder().getId())
                        .criteria(task.getCriteria())
                        .interval(task.getInterval())
                        .nextExecuted(task.getNextExecuted())
                        .build())
                .collect(Collectors.toList());
    }
}
