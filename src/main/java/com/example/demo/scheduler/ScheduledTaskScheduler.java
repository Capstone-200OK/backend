package com.example.demo.scheduler;

import com.example.demo.config.WebSocketSessionManager;
import com.example.demo.entity.Folder;
import com.example.demo.entity.ScheduledTask;
import com.example.demo.repository.ScheduledTaskRepository;
import com.example.demo.service.FolderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ScheduledTaskScheduler {

    private static final String BASE_URL = "http://localhost:8080";
    private final ScheduledTaskRepository taskRepository;
    private final FolderService folderService;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 예약 작업을 주기적으로 실행하는 스케줄러
     * 매 시간 정각마다 실행됨
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void executeScheduledTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledTask> dueTasks = taskRepository.findByNextExecutedLessThanEqual(now);
        Boolean isScheduled = true;

        // 예약 시간이 지난 작업들 실행
        for (ScheduledTask task : dueTasks) {
            try {
                Folder previousFolder = task.getPreviousFolder();
                Folder destinationFolder = task.getNewFolder();

                // 이동 대상 폴더 경로 생성
                String outputPath = folderService.buildFullPath(destinationFolder);

                // REST API 호출을 위한 요청 데이터 생성
                Map<String, Object> body = Map.of(
                        "folderIds", List.of(previousFolder.getId()),
                        "mode", task.getCriteria().name().toLowerCase(),
                        "destinationFolderId", destinationFolder.getId(),
                        "userId", task.getUser().getId(),
                        "output_path", outputPath,
                        "isScheduled", isScheduled,
                        "isMaintain", task.getIsMaintain(),
                        "fileNameChange", task.getFileNameChange()
                );

                // HTTP 요청 헤더 설정
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

                // REST API 호출 (파일 자동 분류 실행)
                restTemplate.postForObject(BASE_URL + "/organize/start", request, String.class);

                System.out.println("[ScheduledTask] Executed: taskId=" + task.getId());

                // WebSocket을 통한 사용자 알림 전송
                Map<String, Object> payload = Map.of(
                        "userId", task.getUser().getId(),
                        "message", String.format("예약한 '%s' 폴더에서 '%s' 폴더로\n자동 정리 되었습니다",
                                previousFolder.getName(), destinationFolder.getName())
                );
                ObjectMapper objectMapper = new ObjectMapper();
                String json = objectMapper.writeValueAsString(payload);

                for (WebSocketSession session : WebSocketSessionManager.getSessions()) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(json));
                    }
                }

                // 다음 실행 시간 갱신
                updateNextExecuted(task, now);
            } catch (Exception e) {
                System.err.println("Error occurred while executing scheduled task: " + e.getMessage());
            }
        }
    }

    /**
     * 다음 실행 시간을 interval 값에 따라 갱신하는 메서드
     *
     * @param task 예약 작업 엔티티
     * @param now 현재 시간
     */
    private void updateNextExecuted(ScheduledTask task, LocalDateTime now) {
        switch (task.getInterval()) {
            case DAILY -> task.setNextExecuted(now.plusDays(1));
            case WEEKLY -> task.setNextExecuted(now.plusWeeks(1));
            case MONTHLY -> task.setNextExecuted(now.plusMonths(1));
        }
        taskRepository.save(task);
    }
}
