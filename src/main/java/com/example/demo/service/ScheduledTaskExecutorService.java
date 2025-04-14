package com.example.demo.service;

import com.example.demo.entity.ScheduledTask;
import com.example.demo.repository.ScheduledTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ScheduledTaskExecutorService {

    private final ScheduledTaskRepository scheduledTaskRepository;
    RestTemplate restTemplate;

    private final String SPRING_TRIGGER_URL = "http://localhost:8080/organize/start";

    @Scheduled(cron = "0 */5 * * * *") // 매 5분마다 실행
    public void checkAndExecuteTasks() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).withMinute((LocalDateTime.now().getMinute() / 5) * 5).withSecond(0).withNano(0);

        List<ScheduledTask> tasksToRun = scheduledTaskRepository.findByNextExecuted(now);
        for (ScheduledTask task : tasksToRun) {
            try {
                // 1. organize/start로 POST 요청
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("folderId", task.getPreviousFolder().getId());

                ResponseEntity<String> response = restTemplate.postForEntity(
                        SPRING_TRIGGER_URL,
                        requestBody,
                        String.class
                );

                System.out.println("자동분류 실행: Task ID " + task.getId() + ", 응답: " + response.getStatusCode());

                // 2. 다음 실행 시간 계산
                LocalDateTime nextTime = getNextExecutionTime(task.getNextExecuted(), task.getInterval().name());
                task.setNextExecuted(nextTime);
                scheduledTaskRepository.save(task);

            } catch (Exception e) {
                System.err.println("자동 실행 실패 (Task ID: " + task.getId() + "): " + e.getMessage());
            }
        }
    }

    private LocalDateTime getNextExecutionTime(LocalDateTime current, String interval) {
        return switch (interval.toUpperCase()) {
            case "DAILY" -> current.plusDays(1);
            case "WEEKLY" -> current.plusWeeks(1);
            case "MONTHLY" -> current.plusMonths(1);
            default -> current;
        };
    }
}
