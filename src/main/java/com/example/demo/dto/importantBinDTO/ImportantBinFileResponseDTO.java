package com.example.demo.dto.importantBinDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportantBinFileResponseDTO {
    private Long importantId;
    private Long fileId;
    private String fileName;
    private String fileType;
    private Long size;
}
