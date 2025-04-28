package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "folders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "parentFolder")
public class Folder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey =@ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private User user;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id")
    private Folder parentFolder;

    @OneToMany(mappedBy = "parentFolder", fetch = FetchType.LAZY)
    private List<Folder> subFolders = new ArrayList<>();

    @CreationTimestamp
    private Timestamp createdAt;

    @Column(name = "folder_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FolderType folderType;

    @Builder.Default
    @Column(name = "is_important", nullable = false)
    private Boolean isImportant = Boolean.FALSE;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;
}
