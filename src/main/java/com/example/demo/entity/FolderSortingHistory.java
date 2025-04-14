package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "folder_sorting_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(FolderSortingHistoryId.class)
public class FolderSortingHistory {

    @Id
    @ManyToOne
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @Id
    @ManyToOne
    @JoinColumn(name = "sorting_id")
    private SortingHistory sorting;

    // "CREATED" 또는 "DELETED" 또는 "MAINTAIN"
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FolderStatus status;
}
