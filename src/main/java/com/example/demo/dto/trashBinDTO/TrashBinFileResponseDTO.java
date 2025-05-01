package com.example.demo.dto.trashBinDTO;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class TrashBinFileResponseDTO {
    private Long trashId;
    private Long fileId;
    private String fileName;
    private String fileType;
    private Long size;
    private Timestamp deletedAt;
}