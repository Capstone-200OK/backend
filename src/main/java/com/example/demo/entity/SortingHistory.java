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

    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @ManyToOne
    @JoinColumn(name = "previous_folder_id", nullable = false)
    private Folder previousFolder;

    @ManyToOne
    @JoinColumn(name = "new_folder_id", nullable = false)
    private Folder newFolder;

    @CreationTimestamp
    private Timestamp sortedAt;
}
