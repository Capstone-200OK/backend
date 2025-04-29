package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationDTO {
    // private Long folderId;
    private String source;
    private String destination;
    private String linkType;
    private Long fileId;
    private String name;
    private String fileType;
    private Long size; // ✅ 반드시 추가
    // 필요한 필드가 더 있으면 추가 (ex: fileId, userId 등)
}