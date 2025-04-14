package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "previous_folder_id", nullable = false)
    private Folder previousFolder;

    @ManyToOne
    @JoinColumn(name = "new_folder_id", nullable = false)
    private Folder newFolder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Criteria criteria;

    @Enumerated(EnumType.STRING)
    @Column(name = "`interval`", nullable = false)
    private ScheduleInterval interval;

    private LocalDateTime nextExecuted;
}

