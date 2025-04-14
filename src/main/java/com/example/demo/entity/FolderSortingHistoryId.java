package com.example.demo.entity;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolderSortingHistoryId implements Serializable {
    private Long folder;
    private Long sorting;
}