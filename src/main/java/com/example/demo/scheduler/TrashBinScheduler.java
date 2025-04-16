package com.example.demo.scheduler;

import com.example.demo.service.TrashBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrashBinScheduler {

    private final TrashBinService trashBinService;

    // 매일 00:00시에 실행
    @Scheduled(cron = "0 0 0 * * *")
    public void autoDeleteExpiredTrash() {
        System.out.println("[TrashBinScheduler] 실행됨: " + java.time.LocalDateTime.now());
        trashBinService.deleteExpiredTrash();
    }
}