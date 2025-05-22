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

    /**
     * 예약 작업을 추가하는 메서드
     *
     * @param scheduledTaskRequestDTO 예약 작업 생성에 필요한 정보가 담긴 DTO
     */
    @Transactional
    public void addScheduledTask(ScheduledTaskRequestDTO scheduledTaskRequestDTO) {
        // 사용자 조회
        User user = userRepository.findById(scheduledTaskRequestDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 이전 폴더 조회
        Folder previousFolder = folderRepository.findById(scheduledTaskRequestDTO.getPreviousFolderId())
                .orElseThrow(() -> new IllegalArgumentException("Previous folder not found"));

        // 이동할 폴더 조회
        Folder newFolder = folderRepository.findById(scheduledTaskRequestDTO.getNewFolderId())
                .orElseThrow(() -> new IllegalArgumentException("New folder not found"));

        // 예약 작업 생성 및 저장
        ScheduledTask scheduledTask = ScheduledTask.builder()
                .user(user)
                .previousFolder(previousFolder)
                .newFolder(newFolder)
                .criteria(scheduledTaskRequestDTO.getCriteria())
                .interval(scheduledTaskRequestDTO.getInterval())
                .nextExecuted(scheduledTaskRequestDTO.getNextExecuted())
                .isMaintain(scheduledTaskRequestDTO.getIsMaintain())
                .fileNameChange(scheduledTaskRequestDTO.getFileNameChange())
                .build();

        scheduledTaskRepository.save(scheduledTask);
    }

    /**
     * 예약 작업을 삭제하는 메서드
     *
     * @param taskId 삭제할 예약 작업의 ID
     */
    @Transactional
    public void deleteScheduledTask(Long taskId) {
        // 예약 작업 존재 여부 확인
        if (!scheduledTaskRepository.existsById(taskId)) {
            throw new IllegalArgumentException("Task not found");
        }

        // 예약 작업 삭제
        scheduledTaskRepository.deleteById(taskId);
    }

    /**
     * 예약 작업을 수정하는 메서드
     *
     * @param taskId 수정할 예약 작업의 ID
     * @param scheduledTaskRequestDTO 수정할 정보가 담긴 DTO
     */
    @Transactional
    public void modifyScheduledTask(Long taskId, ScheduledTaskRequestDTO scheduledTaskRequestDTO) {
        // 예약 작업 조회
        ScheduledTask scheduledTask = scheduledTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // interval 값 수정
        if (scheduledTaskRequestDTO.getInterval() != null) {
            scheduledTask.setInterval(scheduledTaskRequestDTO.getInterval());
        }

        // criteria 값 수정
        if (scheduledTaskRequestDTO.getCriteria() != null) {
            scheduledTask.setCriteria(scheduledTaskRequestDTO.getCriteria());
        }

        // 이전 폴더 수정
        if (scheduledTaskRequestDTO.getPreviousFolderId() != null) {
            Folder previousFolder = folderRepository.findById(scheduledTaskRequestDTO.getPreviousFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("Previous folder not found"));
            scheduledTask.setPreviousFolder(previousFolder);
        }

        // 이동할 폴더 수정
        if (scheduledTaskRequestDTO.getNewFolderId() != null) {
            Folder newFolder = folderRepository.findById(scheduledTaskRequestDTO.getNewFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("New folder not found"));
            scheduledTask.setNewFolder(newFolder);
        }

        // nextExecuted 값 수정
        if (scheduledTaskRequestDTO.getNextExecuted() != null) {
            scheduledTask.setNextExecuted(scheduledTaskRequestDTO.getNextExecuted());
        }

        // 수정된 예약 작업 저장
        scheduledTaskRepository.save(scheduledTask);
    }

    /**
     * 특정 사용자의 예약 작업 목록을 조회하는 메서드
     *
     * @param userId 조회할 사용자의 ID
     * @return 해당 사용자의 예약 작업 리스트
     */
    @Transactional(readOnly = true)
    public List<ScheduledTaskResponseDTO> getScheduledTasksByUser(Long userId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 사용자의 예약 작업 목록 조회
        List<ScheduledTask> tasks = scheduledTaskRepository.findAllByUser(user);

        // 예약 작업 엔티티를 DTO로 변환하여 반환
        return tasks.stream()
                .map(task -> ScheduledTaskResponseDTO.builder()
                        .taskId(task.getId())
                        .userId(task.getUser().getId())
                        .previousFolderId(task.getPreviousFolder().getId())
                        .newFolderId(task.getNewFolder().getId())
                        .previousFolderName(task.getPreviousFolder().getName())
                        .newFolderName(task.getNewFolder().getName())
                        .criteria(task.getCriteria())
                        .interval(task.getInterval())
                        .nextExecuted(task.getNextExecuted())
                        .build())
                .collect(Collectors.toList());
    }
}
