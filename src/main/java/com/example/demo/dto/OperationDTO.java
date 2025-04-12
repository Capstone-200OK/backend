package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationDTO {
    // private Long folderId;
    private String destination;  // 새 경로
    private Long fileId;
    // 필요한 필드가 더 있으면 추가 (ex: fileId, userId 등)
}