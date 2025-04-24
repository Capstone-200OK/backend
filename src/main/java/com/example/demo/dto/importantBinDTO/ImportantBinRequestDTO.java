package com.example.demo.dto.importantBinDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportantBinRequestDTO {
    private Long fileId;
    private Long folderId;
    private Long userId;
}
