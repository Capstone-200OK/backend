package com.example.demo.dto.trashBinDTO;

import lombok.Data;

import java.util.List;

@Data
public class TrashBinRequestDTO {
    private Long userId;
    private List<Long> fileIds;     // 여러 개 가능
    private List<Long> folderIds;   // 여러 개 가능
}
