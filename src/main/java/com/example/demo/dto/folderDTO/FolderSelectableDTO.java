package com.example.demo.dto.folderDTO;

import com.example.demo.entity.FolderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FolderSelectableDTO {
    private Long id;
    private String name;
    private Long parentId;
    private FolderType folderType;
}
