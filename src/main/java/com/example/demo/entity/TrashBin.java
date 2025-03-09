package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.sql.Timestamp;

@Entity
@Table(name = "trash_bin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrashBin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @CreationTimestamp
    private Timestamp deletedAt;
}
