package com.example.demo.entity;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileSortingHistoryId implements Serializable {
    private Long file;
    private Long sorting;
}