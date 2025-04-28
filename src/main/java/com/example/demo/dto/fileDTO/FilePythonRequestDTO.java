package com.example.demo.dto.fileDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilePythonRequestDTO {
    private Long id;
    private String name;
    private String filePath;
    private String fileType;
    private Long size;
    private Boolean isImportant;
    private Boolean isDeleted;
    private Timestamp createdAt;
    private String fileUrl;
    private String fileThumbUrl;
}
