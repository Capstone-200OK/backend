package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizedResultDTO {
    private Long userId;
    private Long folderId;           // 정리 대상 폴더 ID
    private String summary;          // 요약 or 결과 메시지
    private List<OperationDTO> operations; // 이동/변경된 파일·폴더 정보
    private Long destinationFolderId;
    private List<Long> sourceFolderIds;
    private Long size;
    private Boolean isScheduled;
    private List<Long> originalStartFolderIds;
}
