package com.example.demo.controller;

import com.example.demo.dto.OperationDTO;
import com.example.demo.dto.OrganizedResultDTO;
import com.example.demo.dto.fileDTO.MoveRequestDTO;
import com.example.demo.dto.fileDTO.RenameRequestDTO;
import com.example.demo.dto.folderDTO.FolderRequestDTO;
import com.example.demo.dto.folderDTO.FolderResult;
import com.example.demo.dto.sortingHistoryDTO.FileUpdateRequestDTO;
import com.example.demo.dto.sortingHistoryDTO.FolderUpdateRequestDTO;
import com.example.demo.dto.sortingHistoryDTO.SortingHistoryRequestDTO;
import com.example.demo.entity.File;
import com.example.demo.entity.Folder;
import com.example.demo.entity.FolderStatus;
import com.example.demo.repository.FolderRepository;
import com.example.demo.service.FileService;
import com.example.demo.service.FolderService;
import com.example.demo.service.SortingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/organize")
@RequiredArgsConstructor
public class OrganizeController {

    private final RestTemplate restTemplate; // Python 서버와 통신용
    private final String PYTHON_SERVER_URL = "http://localhost:5050"; // Python Flask
    private final FileService fileService;
    private final FolderService folderService;
    private final FolderRepository folderRepository;
    private final SortingHistoryService sortingHistoryService;

    /**
     * 1단계: 사용자가 폴더 선택 후 정리 요청 시작
     * → Python 서버에 정리 요청 전송
     */
    @PostMapping("/start")
    public ResponseEntity<?> startOrganize(@RequestBody Map<String, Object> payload) {
        List<Integer> folderIdsRaw = (List<Integer>) payload.get("folderIds");
        List<Long> folderIds = folderIdsRaw.stream().map(Long::valueOf).toList();
        String sortType = (String) payload.get("mode");
        Long destinationFolderId = ((Number) payload.get("destinationFolderId")).longValue();
        Boolean fileNameChange = (Boolean) payload.get("fileNameChange");

        Folder destFolder = folderRepository.findById(destinationFolderId)
                .orElseThrow(() -> new RuntimeException("Destination folder not found"));

        String outputPath = folderService.buildFullPath(destFolder);

        // Python 서버로 요청 데이터 구성
        Map<String, Object> requestToPython = new HashMap<>();
        requestToPython.put("folderIds", folderIds);
        requestToPython.put("mode", sortType);
        requestToPython.put("output_path", outputPath);
        requestToPython.put("destinationFolderId", destinationFolderId);
        requestToPython.put("userId", payload.get("userId"));
        requestToPython.put("isScheduled", payload.getOrDefault("isScheduled", false));
        requestToPython.put("isMaintain", payload.getOrDefault("isMaintain", false));
        requestToPython.put("fileNameChange", payload.getOrDefault("fileNameChange", false));

        ResponseEntity<Map> response = restTemplate.postForEntity(
                PYTHON_SERVER_URL + "/organize_folder",
                requestToPython,
                Map.class
        );

        // Python 응답 반환
        if (response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.ok(response.getBody());
        } else {
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        }
    }

    /**
     * 2단계: Python → Spring 으로 정리 결과 전송
     * → 파일/폴더 이동 및 기록 저장
     */
    @PostMapping("/result")
    public ResponseEntity<?> handleOrganizedResult(@RequestBody OrganizedResultDTO result) {
        Long userId = result.getUserId();
        Boolean isMaintain = result.getIsMaintain();
        Boolean isScheduled = result.getIsScheduled();
        List<Long> originalIds = result.getOriginalStartFolderIds();

        List<FileUpdateRequestDTO> fileUpdates = new ArrayList<>();
        List<FolderUpdateRequestDTO> folderUpdates = new ArrayList<>();

        Folder parentFolder = folderService.getFolderById(result.getDestinationFolderId());
        String outputPath = folderService.buildFullPath(parentFolder);

        // 폴더 경로 캐시 (중복 생성 방지용)
        Map<String, Folder> folderCache = new HashMap<>();

        for (OperationDTO op : result.getOperations()) {
            try {
                String destPath = op.getDestination().replace("\\", "/");
                String relativePath = extractRelativePath(outputPath, destPath);
                String[] folderNames = relativePath.isEmpty() ? new String[0] : relativePath.split("/");

                Folder currentParent = parentFolder;
                Folder folder = null;
                StringBuilder fullPathKeyBuilder = new StringBuilder();

                for (String name : folderNames) {
                    if (name == null || name.isBlank()) continue;
                    fullPathKeyBuilder.append("/").append(name);
                    String fullPathKey = fullPathKeyBuilder.toString();

                    // 캐시된 폴더가 있으면 재사용
                    if (folderCache.containsKey(fullPathKey)) {
                        currentParent = folderCache.get(fullPathKey);
                        folder = currentParent;
                        continue;
                    }

                    // 새 폴더 생성 또는 검색
                    FolderRequestDTO folderRequest = new FolderRequestDTO();
                    folderRequest.setName(name);
                    folderRequest.setUserId(userId);
                    folderRequest.setParentFolderId(currentParent.getId());
                    folderRequest.setFolderType(parentFolder.getFolderType());

                    FolderResult folderResult = folderService.findOrCreateFolderWithFlag(folderRequest);
                    folder = folderResult.getFolder();
                    currentParent = folder;
                    folderCache.put(fullPathKey, folder);

                    if (folderResult.isNewlyCreated()) {
                        folderUpdates.add(FolderUpdateRequestDTO.builder()
                                .folderId(folder.getId())
                                .status(FolderStatus.CREATED)
                                .build());
                    }
                }

                // 서브 폴더가 없으면 부모 폴더 사용
                if (folder == null) folder = parentFolder;

                File originalFile = fileService.getFileById(op.getFileId());
                String newName = op.getName();

                // 파일 복제 or 이동
                if (Boolean.TRUE.equals(isMaintain)) {
                    File duplicated = fileService.duplicateFile(originalFile, folder, destPath);
                    fileUpdates.add(FileUpdateRequestDTO.builder()
                            .fileId(duplicated.getId())
                            .previousFolderId(originalFile.getFolder().getId())
                            .previousFilePath(originalFile.getFilePath())
                            .build());
                    if (newName != null && !newName.isBlank()) {
                        fileService.renameFile(new RenameRequestDTO(op.getFileId(), newName));
                    }
                } else {
                    fileUpdates.add(FileUpdateRequestDTO.builder()
                            .fileId(op.getFileId())
                            .previousFolderId(originalFile.getFolder().getId())
                            .previousFilePath(originalFile.getFilePath())
                            .build());
                    fileService.moveFile(new MoveRequestDTO(op.getFileId(), folder.getId(), destPath));
                    if (newName != null && !newName.isBlank()) {
                        fileService.renameFile(new RenameRequestDTO(op.getFileId(), newName));
                    }
                }

            } catch (Exception e) {
                System.err.println("Error updating file with fileId " + op.getFileId() + ": " + e.getMessage());
            }
        }

        // 원본 폴더 처리 상태 기록
        for (Long folderId : result.getSourceFolderIds()) {
            Folder originalFolder = folderService.getFolderById(folderId);
            boolean shouldDelete = fileService.countByFolderId(originalFolder.getId()) == 0;

            if (Boolean.TRUE.equals(isMaintain)) continue;

            FolderStatus status;
            if (Boolean.TRUE.equals(isScheduled) && originalIds != null && originalIds.contains(folderId)) {
                status = FolderStatus.MAINTAIN;
            } else {
                status = shouldDelete ? FolderStatus.DELETED : FolderStatus.MAINTAIN;
            }

            folderUpdates.add(FolderUpdateRequestDTO.builder()
                    .folderId(originalFolder.getId())
                    .status(status)
                    .build());
        }

        // 정리 이력 저장
        sortingHistoryService.saveSortingHistory(SortingHistoryRequestDTO.builder()
                .userId(userId)
                .fileUpdates(fileUpdates)
                .folderUpdates(folderUpdates)
                .isMaintain(isMaintain)
                .build());

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("message", "Result processed by Spring");
        return ResponseEntity.ok(responseMap);
    }

    /**
     * 절대 경로에서 outputPath 기준 상대 경로 추출
     */
    private String extractRelativePath(String outputPath, String destPath) {
        int lastSlashIndex = destPath.lastIndexOf("/");
        if (lastSlashIndex == -1) throw new IllegalArgumentException("Invalid path: " + destPath);
        String newFolderPath = destPath.substring(0, lastSlashIndex);
        String relativePath = newFolderPath.replaceFirst(outputPath, "");
        if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
        return relativePath;
    }
}
