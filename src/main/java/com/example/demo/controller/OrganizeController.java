package com.example.demo.controller;

import com.example.demo.dto.OperationDTO;
import com.example.demo.dto.OrganizedResultDTO;
import com.example.demo.dto.sortingHistoryDTO.FileUpdateRequestDTO;
import com.example.demo.dto.fileDTO.MoveRequestDTO;
import com.example.demo.dto.folderDTO.FolderRequestDTO;
import com.example.demo.dto.folderDTO.FolderResult;
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

    // 1) 사용자가 folderId를 선택 → /organize/start 로 POST
    @PostMapping("/start")
    public ResponseEntity<?> startOrganize(@RequestBody Map<String, Object> payload) {
        List<Integer> folderIdsRaw = (List<Integer>) payload.get("folderIds");
        List<Long> folderIds = folderIdsRaw.stream().map(Long::valueOf).toList();
        String sortType = (String) payload.get("mode");
        Long destinationFolderId = ((Number) payload.get("destinationFolderId")).longValue();

        Folder destFolder = folderRepository.findById(destinationFolderId)
                .orElseThrow(() -> new RuntimeException("Destination folder not found"));
        String outputPath = folderService.buildFullPath(destFolder);

        Map<String, Object> requestToPython = new HashMap<>();
        requestToPython.put("folderIds", folderIds); // ✅ 여러 개
        requestToPython.put("mode", sortType);
        requestToPython.put("output_path", outputPath);
        requestToPython.put("destinationFolderId", destinationFolderId); // ✅ 하나
        requestToPython.put("userId", payload.get("userId"));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                PYTHON_SERVER_URL + "/organize_folder",
                requestToPython,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.ok(response.getBody());
        } else {
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        }
    }

    // 2) Python → Spring으로 최종 결과 전송
    //    Python이 /organize/result 로 POST
    @PostMapping("/result")
    public ResponseEntity<?> handleOrganizedResult(@RequestBody OrganizedResultDTO result) {
        System.out.println("Received final result from Python: " + result);
        Long userId = result.getUserId();

        List<FileUpdateRequestDTO> fileUpdates = new ArrayList<>();
        List<FolderUpdateRequestDTO> folderUpdates = new ArrayList<>();

        Folder parentFolder = folderService.getFolderById(result.getDestinationFolderId());
        String outputPath = folderService.buildFullPath(parentFolder); // ex: CloudRoot/CloudTestFolder

        // ✅ 경로 캐시: "text_files/xls_files" → Folder
        Map<String, Folder> folderCache = new HashMap<>();

        for (OperationDTO op : result.getOperations()) {
            try {
                String destPath = op.getDestination().replace("\\", "/");
                int lastSlashIndex = destPath.lastIndexOf("/");
                if (lastSlashIndex == -1) throw new IllegalArgumentException("Invalid path: " + destPath);

                String newFolderPath = destPath.substring(0, lastSlashIndex);  // 폴더 경로까지만
                String relativePath = newFolderPath.replaceFirst(outputPath, "");
                if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);

                String[] folderNames = relativePath.isEmpty() ? new String[0] : relativePath.split("/");

                Folder folder = null;
                Folder currentParent = parentFolder;
                StringBuilder fullPathKeyBuilder = new StringBuilder();

                for (String name : folderNames) {
                    if (name == null || name.isBlank()) continue;

                    fullPathKeyBuilder.append("/").append(name);
                    String fullPathKey = fullPathKeyBuilder.toString();

                    // ✅ 중복 폴더 방지
                    if (folderCache.containsKey(fullPathKey)) {
                        currentParent = folderCache.get(fullPathKey);
                        continue;
                    }

                    FolderRequestDTO folderRequest = new FolderRequestDTO();
                    folderRequest.setName(name);
                    folderRequest.setUserId(userId); // 권한 확인용
                    folderRequest.setParentFolderId(currentParent.getId());
                    folderRequest.setFolderType(parentFolder.getFolderType()); // ✅ 목적지 폴더 기준

                    FolderResult folderResult = folderService.findOrCreateFolderWithFlag(folderRequest);
                    folder = folderResult.getFolder();
                    currentParent = folder;
                    folderCache.put(fullPathKey, folder); // ✅ 캐시 저장

                    if (folderResult.isNewlyCreated()) {
                        folderUpdates.add(FolderUpdateRequestDTO.builder()
                                .folderId(folder.getId())
                                .status(FolderStatus.CREATED)
                                .build());
                    }
                }

                if (folder == null) {
                    folder = parentFolder;  // 하위 폴더가 없는 경우, 목적지 자체로 설정
                }

                File originalFile = fileService.getFileById(op.getFileId());
                fileUpdates.add(FileUpdateRequestDTO.builder()
                        .fileId(op.getFileId())
                        .previousFolderId(originalFile.getFolder().getId())
                        .previousFilePath(originalFile.getFilePath())
                        .build());

                fileService.moveFile(new MoveRequestDTO(op.getFileId(), folder.getId(), destPath));

            } catch (Exception e) {
                System.err.println("Error updating file with fileId " + op.getFileId() + ": " + e.getMessage());
            }
        }

        for (Long folderId : result.getSourceFolderIds()) {
            Folder originalFolder = folderService.getFolderById(folderId);
            boolean shouldDelete = fileService.countByFolderId(originalFolder.getId()) == 0;

            folderUpdates.add(FolderUpdateRequestDTO.builder()
                    .folderId(originalFolder.getId())
                    .status(shouldDelete ? FolderStatus.DELETED : FolderStatus.MAINTAIN)
                    .build());
        }

        sortingHistoryService.saveSortingHistory(SortingHistoryRequestDTO.builder()
                .userId(userId)
                .fileUpdates(fileUpdates)
                .isMaintain(false)
                .folderUpdates(folderUpdates)
                .build());

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("message", "Result processed by Spring");
        return ResponseEntity.ok(responseMap);
    }
}