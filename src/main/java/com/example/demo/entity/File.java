package com.example.demo.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.security.Timestamp;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private Long size;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;
}
