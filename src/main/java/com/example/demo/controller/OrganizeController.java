package com.example.demo.controller;

import com.example.demo.dto.OperationDTO;
import com.example.demo.dto.OrganizedResultDTO;
import com.example.demo.dto.fileDTO.FileUpdateRequestDTO;
import com.example.demo.dto.fileDTO.MoveRequestDTO;
import com.example.demo.dto.folderDTO.FolderRequestDTO;
import com.example.demo.dto.folderDTO.FolderResult;
import com.example.demo.dto.folderDTO.FolderUpdateRequestDTO;
import com.example.demo.dto.sortingHistoryDTO.SortingHistoryRequestDTO;
import com.example.demo.entity.File;
import com.example.demo.entity.Folder;
import com.example.demo.entity.FolderStatus;
import com.example.demo.repository.FolderRepository;
import com.example.demo.service.FileService;
import com.example.demo.service.FolderService;
import com.example.demo.service.SortingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.regex.Pattern;

@RestController
@RequestMapping("/organize")
@RequiredArgsConstructor
public class OrganizeController {

    private final RestTemplate restTemplate; // Python 서버와 통신용
    private final String PYTHON_SERVER_URL = "http://localhost:5000"; // Python Flask
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
        Long userId = 1L; // 예시: 실제 사용자 ID는 인증 정보를 통해 가져오거나 Spring에서 전달받음

        List<FileUpdateRequestDTO> fileUpdates = new ArrayList<>();
        List<FolderUpdateRequestDTO> folderUpdates = new ArrayList<>();

        // 각 OperationDTO를 순회하며 파일의 폴더 업데이트(이동) 처리
        for (OperationDTO op : result.getOperations()) {
            try {
                // 1. destination 경로에서 새 폴더 경로를 파싱 (예: "/organized/text_files/xls_files/test.xlsx" 에서 폴더 부분)
                String destPath = op.getDestination();
                Long folderId = result.getDestinationFolderId();
                //Folder folder1 = folderService.getFolderById(folderId);
                // 폴더 경로는 파일명 제외한 경로
                String normalizedPath = destPath.replace("\\", "/");  // 경로 통일
                int lastSlashIndex = normalizedPath.lastIndexOf("/");
                if (lastSlashIndex == -1) {
                    throw new IllegalArgumentException("Invalid path: " + destPath);
                }
                // 2. newFolderPath를 기반으로 폴더 정보를 생성하거나 검색
                // 예를 들어, 폴더 경로의 마지막 폴더명을 이용하여 FolderRequestDTO를 구성
                //FolderRequestDTO folderRequest = new FolderRequestDTO();
                // newFolderPath를 경로 구분자로 분리해서 마지막 부분(새 폴더 이름) 선택.
                String newFolderPath = normalizedPath.substring(0, lastSlashIndex);  // 마지막 / 전까지 폴더경로
                String[] folderNames = newFolderPath.split("/");
                Long parentId = null;  // Root 폴더 ID
                Folder folder = null;
                for (String name : folderNames) {
                    if (name == null || name.isBlank()) continue;

                    FolderRequestDTO folderRequest = new FolderRequestDTO();
                    folderRequest.setName(name);
                    folderRequest.setUserId(userId);
                    folderRequest.setParentFolderId(parentId);

                    FolderResult folderResult = folderService.findOrCreateFolderWithFlag(folderRequest);
                    folder = folderResult.getFolder();
                    parentId = folder.getId(); // 다음 폴더의 parent로 연결
                    if (folderResult.isNewlyCreated()) {
                        FolderUpdateRequestDTO folderUpdate = FolderUpdateRequestDTO.builder()
                                .folderId(folder.getId())
                                .status(FolderStatus.CREATED)
                                .build();
                        folderUpdates.add(folderUpdate);
                    }
                }
                // 만약 상위 폴더 정보도 있다면 추가 (예: parentFolderId)
                // folderRequest.setParentFolderId(...);

                if (folder == null) {
                    throw new IllegalStateException("Folder creation failed for path: " + newFolderPath);
                }

                // 3. MoveRequestDTO를 생성하여 파일의 folderId, filePath 업데이트
            /*  */

                System.out.println("Updated fileId " + op.getFileId() + " to folderId " + folder.getId());
                File originalFile = fileService.getFileById(op.getFileId());
                FileUpdateRequestDTO fileUpdate = FileUpdateRequestDTO.builder()
                        .fileId(op.getFileId())
                        .newName(op.getName())  // 필요시 OperationDTO에 name도 넣도록
                        .newFolderId(originalFile.getFolder().getId())
                        .newFilePath(originalFile.getFilePath())
                        .build();
                fileUpdates.add(fileUpdate);
                System.out.println("fileUpdate: " + fileUpdate);
                MoveRequestDTO moveRequest = new MoveRequestDTO(op.getFileId(), folder.getId(), destPath);
                System.out.println(moveRequest);
                fileService.moveFile(moveRequest);
            } catch (Exception e) {
                System.err.println("Error updating file with fileId " + op.getFileId() + ": " + e.getMessage());
            }
        }
        for (Long folderId : result.getSourceFolderIds()) {
            Folder originalFolder = folderService.getFolderById(folderId);
            boolean shouldDelete = fileService.countByFolderId(originalFolder.getId()) == 0;

            FolderUpdateRequestDTO folderUpdate = FolderUpdateRequestDTO.builder()
                    .folderId(originalFolder.getId())
                    .status(shouldDelete ? FolderStatus.DELETED : FolderStatus.MAINTAIN)
                    .build();

            folderUpdates.add(folderUpdate);
        }


        SortingHistoryRequestDTO historyRequest = SortingHistoryRequestDTO.builder()
                .userId(userId)
                .fileUpdates(fileUpdates)
                .folderUpdates(folderUpdates)
                .build();

        sortingHistoryService.saveSortingHistory(historyRequest);
        // 추가: 만약 원래 사용되던 폴더(예: 자동분류 대상 폴더)는 삭제 처리해야 한다면, folderService.markFolderAsDeleted() 호출 등 추가 작업
        // folderService.markFolderAsDeleted(result.getFolderId());

        // 최종적으로 DB 업데이트가 완료되었음을 반환
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("message", "Result processed by Spring");
        return ResponseEntity.ok(responseMap);
    }
}