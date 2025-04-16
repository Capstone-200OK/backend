package com.example.demo.scheduler;

import com.example.demo.entity.ScheduledTask;
import com.example.demo.repository.ScheduledTaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ScheduledTaskScheduler {

    private final ScheduledTaskRepository taskRepository;
    private final RestTemplate restTemplate = new RestTemplate(); // 혹은 @Bean으로 등록해도 OK

    private static final String BASE_URL = "http://localhost:8080"; // 실제 Base_URL로 수정

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void executeScheduledTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledTask> dueTasks = taskRepository.findByNextExecutedBeforeOrNextExecutedEquals(now, now);

        for (ScheduledTask task : dueTasks) {
            try {
                String apiUrl = BASE_URL + "/organize/start";

                Map<String, Object> body = Map.of(
                        "folderId", task.getPreviousFolder().getId(),
                        "mode", task.getCriteria().name().toLowerCase(), // 소문자, 대문자 해결하기
                        "destinationFolderId", task.getNewFolder().getId()
                );

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

                restTemplate.postForObject(apiUrl, request, String.class);

                System.out.println("[ScheduledTask] 실행됨: taskId=" + task.getId());

                // 실행 이후 다음 실행일 갱신 (예: interval 기준)
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
