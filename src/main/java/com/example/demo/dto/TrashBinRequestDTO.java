package com.example.demo.dto;

import lombok.Data;

@Data
public class TrashBinRequestDTO {
    private Long userId;
    private Long fileId;     // optional
    private Long folderId;   // optional
}
