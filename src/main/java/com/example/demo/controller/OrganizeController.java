package com.example.demo.controller;

import com.example.demo.dto.OperationDTO;
import com.example.demo.dto.OrganizedResultDTO;
import com.example.demo.dto.fileDTO.MoveRequestDTO;
import com.example.demo.dto.folderDTO.FolderRequestDTO;
import com.example.demo.entity.Folder;
import com.example.demo.service.FileService;
import com.example.demo.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.HashMap;
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
    // 1) 사용자가 folderId를 선택 → /organize/start 로 POST
    @PostMapping("/start")
    public ResponseEntity<?> startOrganize(@RequestBody Map<String, Object> payload) {
        // 예: payload {"folderId": 2}
        Long folderId = ((Number) payload.get("folderId")).longValue();
        String sortType = (String) payload.get("mode");
        String sortPath = (String) payload.get("output_path");

        // Python에게 /organize_folder 호출해 "폴더 자동분류"를 요청
        Map<String, Object> requestToPython = new HashMap<>();
        requestToPython.put("folderId", folderId);
        requestToPython.put("mode", sortType);
        requestToPython.put("output_path", sortPath);

        // Python 서버로 POST
        ResponseEntity<Map> response = restTemplate.postForEntity(
                PYTHON_SERVER_URL + "/organize_folder",
                requestToPython,
                Map.class
        );

        // Python이 {"message":"Organize done"} 등 응답
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

        // 각 OperationDTO를 순회하며 파일의 폴더 업데이트(이동) 처리
        for (OperationDTO op : result.getOperations()) {
            try {
                // 1. destination 경로에서 새 폴더 경로를 파싱 (예: "/organized/text_files/xls_files/test.xlsx" 에서 폴더 부분)
                String destPath = op.getDestination();
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
                Long parentId = 1L;  // Root 폴더 ID
                Folder folder = null;
                for (String name : folderNames) {
                    if (name == null || name.isBlank()) continue;

                    FolderRequestDTO folderRequest = new FolderRequestDTO();
                    folderRequest.setName(name);
                    folderRequest.setUserId(userId);
                    folderRequest.setParentFolderId(parentId);

                    folder = folderService.findOrCreateFolder(folderRequest);
                    parentId = folder.getId(); // 다음 폴더의 parent로 연결
                }
                // 만약 상위 폴더 정보도 있다면 추가 (예: parentFolderId)
                // folderRequest.setParentFolderId(...);

                if (folder == null) {
                    throw new IllegalStateException("Folder creation failed for path: " + newFolderPath);
                }

                // 3. MoveRequestDTO를 생성하여 파일의 folderId, filePath 업데이트
                MoveRequestDTO moveRequest = new MoveRequestDTO(op.getFileId(), folder.getId(), destPath);
                System.out.println(moveRequest);
                fileService.moveFile(moveRequest);

                System.out.println("Updated fileId " + op.getFileId() + " to folderId " + folder.getId());
            } catch (Exception e) {
                System.err.println("Error updating file with fileId " + op.getFileId() + ": " + e.getMessage());
            }
        }

        // 추가: 만약 원래 사용되던 폴더(예: 자동분류 대상 폴더)는 삭제 처리해야 한다면, folderService.markFolderAsDeleted() 호출 등 추가 작업
        // folderService.markFolderAsDeleted(result.getFolderId());

        // 최종적으로 DB 업데이트가 완료되었음을 반환
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("message", "Result processed by Spring");
        return ResponseEntity.ok(responseMap);
    }
}