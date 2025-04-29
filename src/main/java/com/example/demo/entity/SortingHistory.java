package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;


@Entity
@Table(name = "sorting_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SortingHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    private Timestamp sortedAt;

    @Builder.Default
    @Column(name = "is_maintain", nullable = false)
    private Boolean isMaintain = Boolean.FALSE;
}

