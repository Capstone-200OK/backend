package com.example.demo.dto.importantBinDTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportantBinResponseDTO {
    private Long importantId;
    private Long fileId;
    private Long folderId;
}
