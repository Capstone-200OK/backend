package com.example.demo.service;

import com.example.demo.dto.fileDTO.*;
import com.example.demo.dto.folderDTO.FolderPathResponseDTO;
import com.example.demo.entity.*;
import com.example.demo.repository.FileRepository;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.FolderPermissionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final FolderAccessService folderAccessService;
    private final S3Uploader s3Uploader;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Transactional
    public File uploadFile(FileRequestDTO fileRequestDTO, MultipartFile multipartFile) {
        User user = userRepository.findById(fileRequestDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Folder folder = folderRepository.findById(fileRequestDTO.getFolderId())
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        //  권한 체크: Personal / Cloud 분기
        if (folder.getFolderType() == FolderType.PERSONAL) {
            if (!isRootPersonalFolder(folder)) {
                // ✅ 루트폴더가 아니면 userId 매칭 필수
                if (!folder.getUser().getId().equals(user.getId())) {
                    throw new RuntimeException("You do not have permission to upload to this personal folder.");
                }
            }
        } else {
            if (!folderAccessService.canWrite(user, folder)) {
                throw new RuntimeException("You do not have permission to upload to this cloud folder.");
            }
        }

        // 파일명 중복 처리
        String finalName = resolveDuplicateName(folder.getId(), user.getId(), fileRequestDTO.getName(), folder.getFolderType());

        // 경로 재구성
        String newFilePath = replaceFileNameInPath(fileRequestDTO.getFilePath(), finalName);

        // S3 업로드
        String s3Url = s3Uploader.upload(multipartFile, bucket, "uploads");

        // 썸네일 생성 요청
        RestTemplate restTemplate = new RestTemplate();
        String thumbnailApi = "http://localhost:5050/api/thumbnail";

        Map<String, String> request = new HashMap<>();
        request.put("fileUrl", s3Url);
        request.put("fileName", finalName);

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
    private boolean isRootPersonalFolder(Folder folder) {
        return folder.getParentFolder() == null && folder.getFolderType() == FolderType.PERSONAL;
    }
    private String replaceFileNameInPath(String originalPath, String newFileName) {
        int lastSlash = originalPath.lastIndexOf('/');
        if (lastSlash == -1) return newFileName;
        return originalPath.substring(0, lastSlash + 1) + newFileName;
    }

    private String resolveDuplicateName(Long folderId, Long userId, String originalName, FolderType folderType) {
        String baseName = originalName;
        String extension = "";

        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex != -1) {
            baseName = originalName.substring(0, dotIndex);
            extension = originalName.substring(dotIndex);
        }

        String newName = originalName;
        int counter = 1;

        if (folderType == FolderType.PERSONAL) {
            // 개인 폴더 ➔ userId + folderId + 파일 이름 기준으로 검사
            while (fileRepository.existsByFolderIdAndUserIdAndNameAndIsDeletedFalse(folderId, userId, newName)) {
                newName = baseName + "(" + counter++ + ")" + extension;
            }
        } else {
            // 클라우드 폴더 ➔ folderId + 파일 이름 기준으로 검사 (userId 신경 X)
            while (fileRepository.existsByFolderIdAndNameAndIsDeletedFalse(folderId, newName)) {
                newName = baseName + "(" + counter++ + ")" + extension;
            }
        }

        return newName;
    }


    @Transactional
    public void moveFile(MoveRequestDTO moveRequestDTO) {
        File file = fileRepository.findById(moveRequestDTO.getFileId())
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        Folder targetFolder = folderRepository.findById(moveRequestDTO.getFolderId())
                .orElseThrow(() -> new IllegalArgumentException("Target folder not found"));

        User user = file.getUser();

        //  이동 권한 체크
           if (targetFolder.getFolderType() == FolderType.PERSONAL) {
               if (!isRootFolder(targetFolder)) {
                   if (!targetFolder.getUser().getId().equals(user.getId())) {
                       throw new RuntimeException("You do not have permission to move to this personal folder.");
                   }
               }
           }
        else if (targetFolder.getFolderType() == FolderType.CLOUD) {
            if (!folderAccessService.canWrite(user, targetFolder)) {
                throw new RuntimeException("You do not have write permission for this cloud folder.");
            }
        }

        file.setFilePath(moveRequestDTO.getFilePath());
        file.setFolder(targetFolder);
    }
    private boolean isRootFolder(Folder folder) {
        return folder.getId() == 1L;
    }
    @Transactional
    public void renameFile(RenameRequestDTO renameRequestDTO) {
        File file = fileRepository.findById(renameRequestDTO.getFileId())
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        file.setName(renameRequestDTO.getNewName());
//        file.setFilePath(renameRequestDTO.getNewFilePath());
    }

    @Transactional(readOnly = true)
    public List<File> getFilesByFolder(Long folderId, Long userId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        //  권한 체크
        if (folder.getFolderType() == FolderType.PERSONAL) {
            if (!folder.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("You do not have permission to view this personal folder.");
            }
        } else if (folder.getFolderType() == FolderType.CLOUD) {
            if (!folderAccessService.canRead(user, folder)) {
                throw new RuntimeException("You do not have read permission for this cloud folder.");
            }
        }

        //  권한 통과 후 정상 조회
        return fileRepository.findByFolderIdAndIsDeletedFalse(folderId);
    }


    @Transactional
    public void deleteFile(DeleteRequestDTO deleteRequestDTO) {
        File file = fileRepository.findById(deleteRequestDTO.getFileId())
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        Folder folder = file.getFolder();
        User user = file.getUser(); // 파일 작성자 기준 (필요시 수정 가능)

        //  삭제 권한 체크
        if (folder.getFolderType() == FolderType.PERSONAL) {
            if (!folder.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("You do not have permission to delete this personal file.");
            }
        } else if (folder.getFolderType() == FolderType.CLOUD) {
            if (!folderAccessService.canDelete(user, folder)) {
                throw new RuntimeException("You do not have delete permission for this cloud file.");
            }
        }

        file.setIsDeleted(true);
    }

    public long countByFolderId(Long folderId) {
        return fileRepository.countByFolderIdAndIsDeletedFalse(folderId);
    }

    public File getFileById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    @Transactional
    public File duplicateFile(File originalFile, Folder destinationFolder, String newFilePath) {
        // 파일명 중복 처리
        String originalName = originalFile.getName();
        Long userId = originalFile.getUser().getId();
        Long folderId = destinationFolder.getId();
        FolderType folderType = destinationFolder.getFolderType();

        String finalName = resolveDuplicateName(folderId, userId, originalName, folderType);

        File duplicated = File.builder()
                .user(originalFile.getUser())
                .folder(destinationFolder)
                .name(finalName)
                .fileType(originalFile.getFileType())
                .filePath(newFilePath)
                .size(originalFile.getSize())
                .fileUrl(originalFile.getFileUrl())
                .fileThumbUrl(originalFile.getFileThumbUrl())
                .isDeleted(false)
                .isImportant(originalFile.getIsImportant())
                .build();

        return fileRepository.save(duplicated);
    }

    public List<FileSearchResponseDTO> searchFolder(Long userId, String input) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<FileSearchResponseDTO> files = new ArrayList<>();

        List<File> fileList = fileRepository.findAllByNameContainingIgnoreCaseAndIsDeletedFalseAndUser(input, user);
        for (File file : fileList) {
            files.add(FileSearchResponseDTO.builder()
                    .fileId(file.getId())
                    .fileName(file.getName())
                    .parentFolderId(file.getFolder().getId())
                    .parentFolderName(file.getFolder().getName())
                    .folderType(file.getFolder().getFolderType())
                    .build());
        }

        return files;
    }
}
