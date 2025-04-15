package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "file_sorting_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(FileSortingHistoryId.class)
public class FileSortingHistory {

    @Id
    @ManyToOne
    @JoinColumn(name = "file_id")
    private File file;

    @Id
    @ManyToOne
    @JoinColumn(name = "sorting_id")
    private SortingHistory sorting;

    @ManyToOne
    @JoinColumn(name = "previous_folder_id")
    private Folder previousFolder;

    @ManyToOne
    @JoinColumn(name = "new_folder_id")
    private Folder newFolder;

    @JoinColumn(name = "previous_file_path")
    private String previousFilePath;
}
