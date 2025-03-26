package com.example.demo.dto;

import com.example.demo.entity.Folder;
import com.example.demo.entity.User;
import lombok.Data;

@Data
public class FolderRequestDTO {
    private Long userId;
    private String name;
    private Long parentFolderId;
}
