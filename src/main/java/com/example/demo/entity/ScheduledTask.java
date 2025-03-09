package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.sql.Timestamp;

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
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleInterval interval;

    private Timestamp lastExecuted;

    @CreationTimestamp
    private Timestamp createdAt;
}

