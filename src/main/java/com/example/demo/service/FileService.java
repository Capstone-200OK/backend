package com.example.demo.service;

import com.example.demo.dto.fileDTO.DeleteRequestDTO;
import com.example.demo.dto.fileDTO.FileRequestDTO;
import com.example.demo.dto.fileDTO.MoveRequestDTO;
import com.example.demo.dto.fileDTO.RenameRequestDTO;
import com.example.demo.entity.File;
import com.example.demo.entity.Folder;
import com.example.demo.entity.User;
import com.example.demo.repository.FileRepository;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final S3Uploader s3Uploader;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Transactional
    public File uploadFile(FileRequestDTO fileRequestDTO, MultipartFile multipartFile) {
        User user = userRepository.findById(fileRequestDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Folder folder = folderRepository.findById(fileRequestDTO.getFolderId())
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        // 중복 처리된 최종 파일 이름
        String finalName = resolveDuplicateName(folder.getId(), fileRequestDTO.getName());

        // 원래 filePath의 디렉토리 부분 유지, 파일명만 교체
        String newFilePath = replaceFileNameInPath(fileRequestDTO.getFilePath(), finalName);

        String s3Url = s3Uploader.upload(multipartFile, bucket, "uploads");

        // 썸네일 요청
        RestTemplate restTemplate = new RestTemplate();
        String thumbnailApi = "http://localhost:5050/api/thumbnail"; // Python 서버 주소

        Map<String, String> request = new HashMap<>();
        request.put("fileUrl", s3Url); // 원본 파일 S3 URL
        request.put("fileName", finalName); // 예: report.docx

        ResponseEntity<Map> response = restTemplate.postForEntity(thumbnailApi, request, Map.class);
        String thumbnailUrl = (String) response.getBody().get("thumbnailUrl");


        File file = File.builder()
                .user(user)
                .folder(folder)
                .name(finalName)
                .filePath(newFilePath)
                .fileType(fileRequestDTO.getFileType())
                .size(fileRequestDTO.getSize())
                .isDeleted(false)
                .fileUrl(s3Url)
                .fileThumbUrl(thumbnailUrl)
                .build();

        return fileRepository.save(file);
    }
    private String replaceFileNameInPath(String originalPath, String newFileName) {
        int lastSlash = originalPath.lastIndexOf('/');
        if (lastSlash == -1) return newFileName; // fallback
        return originalPath.substring(0, lastSlash + 1) + newFileName;
    }
    private String resolveDuplicateName(Long folderId, String originalName) {
        String baseName = originalName;
        String extension = "";

        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex != -1) {
            baseName = originalName.substring(0, dotIndex);
            extension = originalName.substring(dotIndex);
        }

        String newName = originalName;
        int counter = 1;

        while (fileRepository.existsByFolderIdAndNameAndIsDeletedFalse(folderId, newName)) {
            newName = baseName + "(" + counter + ")" + extension;
            counter++;
        }

        return newName;
    }

    public long countByFolderId(Long folderId) {
        return fileRepository.countByFolderIdAndIsDeletedFalse(folderId);
    }
    public File getFileById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
    }
    @Transactional
    public void moveFile(MoveRequestDTO moveRequestDTO) {
        File file = fileRepository.findById(moveRequestDTO.getFileId())
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        file.setFilePath(moveRequestDTO.getFilePath());
        file.setFolder(folderRepository.findById(moveRequestDTO.getFolderId()).
                orElseThrow(() -> new IllegalArgumentException("Folder not found")));
    }
    @Transactional
    public void renameFile(RenameRequestDTO renameRequestDTO) {
        File file = fileRepository.findById(renameRequestDTO.getFileId())
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        file.setName(renameRequestDTO.getNewName());
        file.setFilePath(renameRequestDTO.getNewFilePath());
    }
    @Transactional(readOnly = true)
    public List<File> getFilesByFolder(Long folderId) {
        return fileRepository.findByFolderIdAndIsDeletedFalse(folderId);
    }

/*    @Transactional
    public void deleteFile(DeleteRequestDTO deleteRequestDTO) {
        File file = fileRepository.findById(deleteRequestDTO.getFileId())
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        file.setIsDeleted(true);
    }*/
}
