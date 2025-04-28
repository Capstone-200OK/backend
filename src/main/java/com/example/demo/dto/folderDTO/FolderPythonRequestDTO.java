package com.example.demo.dto.folderDTO;

import com.example.demo.dto.fileDTO.FilePythonRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolderPythonRequestDTO {
    private Long id;
    private String name;
    private Boolean isDeleted;
    private Boolean isImportant;
    private List<FolderPythonRequestDTO> subFolders = new ArrayList<>();
    private List<FilePythonRequestDTO> files = new ArrayList<>();
}
