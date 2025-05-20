package com.example.demo.controller;

import com.example.demo.dto.fileDTO.FileRequestDTO;
import com.example.demo.dto.fileDTO.FileSearchResponseDTO;
import com.example.demo.dto.fileDTO.MoveRequestDTO;
import com.example.demo.dto.fileDTO.RenameRequestDTO;
import com.example.demo.entity.File;
import com.example.demo.service.FileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 폴더 내 파일 개수 반환
     *
     * @param id 폴더 ID
     * @return 파일 개수
     */
    @GetMapping("/fileCount")
    public Long fileCount(@RequestParam Long id) {
        return fileService.countByFolderId(id);
    }

    /**
     * 파일 업로드 처리
     *
     * @param metaJson 메타데이터(JSON 문자열)
     * @param multipartFile 업로드할 실제 파일
     * @return 업로드된 파일 DTO
     */
    @PostMapping("/upload")
    public FileDTO uploadFile(
            @RequestParam("meta") String metaJson,
            @RequestPart("file") MultipartFile multipartFile) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        FileRequestDTO dto = objectMapper.readValue(metaJson, FileRequestDTO.class);
        File file = fileService.uploadFile(dto, multipartFile);
        return FileDTO.fromEntity(file);
    }

    /**
     * 파일 이름 변경
     *
     * @param renameRequestDTO 변경할 이름 정보
     */
    @PostMapping("/rename")
    public void renameFile(@RequestBody RenameRequestDTO renameRequestDTO) {
        fileService.renameFile(renameRequestDTO);
    }

    /**
     * 파일 이동
     *
     * @param moveRequestDTO 이동할 대상 정보
     */
    @PostMapping("/move")
    public void moveFile(@RequestBody MoveRequestDTO moveRequestDTO) {
        fileService.moveFile(moveRequestDTO);
    }

    /**
     * 폴더 내 파일 목록 조회
     *
     * @param folderId 폴더 ID
     * @param userId 사용자 ID
     * @return 파일 DTO 리스트
     */
    @GetMapping("/list")
    public ResponseEntity<List<FileDTO>> getFilesByFolder(@RequestParam Long folderId, @RequestParam Long userId) {
        List<File> files = fileService.getFilesByFolder(folderId, userId);
        List<FileDTO> response = files.stream()
                .map(FileDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 기준으로 파일 검색
     *
     * @param userId 사용자 ID
     * @param input 검색어
     * @return 검색된 파일 목록
     */
    @GetMapping("/search/{userId}/{input}")
    public ResponseEntity<List<FileSearchResponseDTO>> searchFolder(@PathVariable Long userId, @PathVariable String input) {
        List<FileSearchResponseDTO> folders = fileService.searchFolder(userId, input);
        return ResponseEntity.ok(folders);
    }

    /**
     * 클라이언트에 전달할 파일 DTO
     */
    public record FileDTO(Long id, String name, Long folderId, Boolean isImportant) {
        public static FileDTO fromEntity(File file) {
            return new FileDTO(
                    file.getId(),
                    file.getName(),
                    file.getFolder() == null ? null : file.getFolder().getId(),
                    file.getIsImportant()
            );
        }
    }
}
