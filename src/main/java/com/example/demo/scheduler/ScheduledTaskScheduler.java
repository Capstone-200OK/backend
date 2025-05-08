package com.example.demo.scheduler;

import com.example.demo.entity.Folder;
import com.example.demo.entity.ScheduledTask;
import com.example.demo.repository.ScheduledTaskRepository;
import com.example.demo.service.FolderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import com.example.demo.config.WebSocketSessionManager;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ScheduledTaskScheduler {

    private final ScheduledTaskRepository taskRepository;
    private final FolderService folderService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BASE_URL = "http://localhost:8080";

    @Scheduled(cron = "0 0 * * * *") //
    @Transactional
    public void executeScheduledTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledTask> dueTasks = taskRepository.findByNextExecutedLessThanEqual(now);
        Boolean isScheduled = true;

        for (ScheduledTask task : dueTasks) {
            try {
                Folder previousFolder = task.getPreviousFolder();
                Folder destinationFolder = task.getNewFolder();

                String outputPath = folderService.buildFullPath(destinationFolder);

                Map<String, Object> body = Map.of(
                        "folderIds", List.of(previousFolder.getId()),  // ✅ 리스트로 전달
                        "mode", task.getCriteria().name().toLowerCase(),
                        "destinationFolderId", destinationFolder.getId(),
                        "userId", task.getUser().getId(),
                        "output_path", outputPath,
                        "isScheduled", isScheduled
                );

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

                restTemplate.postForObject(BASE_URL + "/organize/start", request, String.class);

                System.out.println("[ScheduledTask] 실행됨: taskId=" + task.getId());

                // ✅ WebSocket 알림 전송
                Map<String, Object> payload = Map.of(
                        "userId", task.getUser().getId(),
                        "message", String.format("'%s' 폴더가 '%s'로 이동되었습니다.", previousFolder.getName(), destinationFolder.getName())
                );
                ObjectMapper objectMapper = new ObjectMapper();
                String json = objectMapper.writeValueAsString(payload);

                for (WebSocketSession session : WebSocketSessionManager.getSessions()) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(json));
                    }
                }

                updateNextExecuted(task, now);
            } catch (Exception e) {
                System.err.println("예약 작업 실행 중 오류: " + e.getMessage());
            }
        }
    }

    private void updateNextExecuted(ScheduledTask task, LocalDateTime now) {
        switch (task.getInterval()) {
            case DAILY -> task.setNextExecuted(now.plusDays(1));
            case WEEKLY -> task.setNextExecuted(now.plusWeeks(1));
            case MONTHLY -> task.setNextExecuted(now.plusMonths(1));
        }
        taskRepository.save(task);
    }
}
