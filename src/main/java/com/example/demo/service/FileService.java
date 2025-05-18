package com.example.demo.service;

import com.example.demo.dto.fileDTO.*;
import com.example.demo.entity.File;
import com.example.demo.entity.Folder;
import com.example.demo.entity.FolderType;
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

import java.util.ArrayList;
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

    /**
     * 파일 업로드 및 저장
     *
     * @param fileRequestDTO 업로드 요청 정보
     * @param multipartFile  업로드 파일
     * @return 저장된 파일 엔티티
     */
    @Transactional
    public File uploadFile(FileRequestDTO fileRequestDTO, MultipartFile multipartFile) {
        User user = userRepository.findById(fileRequestDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Folder folder = folderRepository.findById(fileRequestDTO.getFolderId())
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        // 권한 체크: 개인/클라우드 폴더 구분
        if (folder.getFolderType() == FolderType.PERSONAL) {
            if (!isRootPersonalFolder(folder) && !folder.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("You do not have permission to upload to this personal folder.");
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

        // S3에 파일 업로드
        String s3Url = s3Uploader.upload(multipartFile, bucket, "uploads");

        // 썸네일 생성 요청
        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> request = Map.of("fileUrl", s3Url, "fileName", finalName);
        ResponseEntity<Map> response = restTemplate.postForEntity("http://localhost:5050/api/thumbnail", request, Map.class);
        String thumbnailUrl = (String) response.getBody().get("thumbnailUrl");

        // 파일 엔티티 생성 및 저장
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

    // 루트 개인 폴더인지 확인
    private boolean isRootPersonalFolder(Folder folder) {
        return folder.getParentFolder() == null && folder.getFolderType() == FolderType.PERSONAL;
    }

    // 경로의 파일 이름만 바꾸기
    private String replaceFileNameInPath(String originalPath, String newFileName) {
        int lastSlash = originalPath.lastIndexOf('/');
        if (lastSlash == -1) return newFileName;
        return originalPath.substring(0, lastSlash + 1) + newFileName;
    }

    // 중복 파일명 처리 로직
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
            while (fileRepository.existsByFolderIdAndUserIdAndNameAndIsDeletedFalse(folderId, userId, newName)) {
                newName = baseName + "(" + counter++ + ")" + extension;
            }
        } else {
            while (fileRepository.existsByFolderIdAndNameAndIsDeletedFalse(folderId, newName)) {
                newName = baseName + "(" + counter++ + ")" + extension;
            }
        }

        return newName;
    }

    /**
     * 파일 이동
     *
     * @param moveRequestDTO 이동 요청 정보
     */
    @Transactional
    public void moveFile(MoveRequestDTO moveRequestDTO) {
        File file = fileRepository.findById(moveRequestDTO.getFileId())
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        Folder targetFolder = folderRepository.findById(moveRequestDTO.getFolderId())
                .orElseThrow(() -> new IllegalArgumentException("Target folder not found"));

        User user = file.getUser();

        // 이동 권한 체크
        if (targetFolder.getFolderType() == FolderType.PERSONAL &&
                !isRootFolder(targetFolder) &&
                !targetFolder.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You do not have permission to move to this personal folder.");
        } else if (targetFolder.getFolderType() == FolderType.CLOUD &&
                !folderAccessService.canWrite(user, targetFolder)) {
            throw new RuntimeException("You do not have write permission for this cloud folder.");
        }

        file.setFilePath(moveRequestDTO.getFilePath());
        file.setFolder(targetFolder);
    }

    private boolean isRootFolder(Folder folder) {
        return folder.getId() == 1L;
    }

    /**
     * 파일 이름 변경
     *
     * @param renameRequestDTO 이름 변경 요청 정보
     */
    @Transactional
    public void renameFile(RenameRequestDTO renameRequestDTO) {
        File file = fileRepository.findById(renameRequestDTO.getFileId())
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        file.setName(renameRequestDTO.getNewName());
    }

    /**
     * 특정 폴더 내 파일 조회
     *
     * @param folderId 폴더 ID
     * @param userId   사용자 ID
     * @return 파일 리스트
     */
    @Transactional(readOnly = true)
    public List<File> getFilesByFolder(Long folderId, Long userId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 권한 체크
        if (folder.getFolderType() == FolderType.PERSONAL &&
                !folder.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You do not have permission to view this personal folder.");
        } else if (folder.getFolderType() == FolderType.CLOUD &&
                !folderAccessService.canRead(user, folder)) {
            throw new RuntimeException("You do not have read permission for this cloud folder.");
        }

        return fileRepository.findByFolderIdAndIsDeletedFalse(folderId);
    }

    /**
     * 폴더 내 삭제되지 않은 파일 개수 조회
     *
     * @param folderId 폴더 ID
     * @return 파일 개수
     */
    public long countByFolderId(Long folderId) {
        return fileRepository.countByFolderIdAndIsDeletedFalse(folderId);
    }

    /**
     * 파일 ID로 파일 조회
     *
     * @param id 파일 ID
     * @return 파일 엔티티
     */
    public File getFileById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    /**
     * 파일 복제
     *
     * @param originalFile      원본 파일
     * @param destinationFolder 복사 대상 폴더
     * @param newFilePath       새로운 경로
     * @return 복제된 파일
     */
    @Transactional
    public File duplicateFile(File originalFile, Folder destinationFolder, String newFilePath) {
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

    /**
     * 사용자 파일 검색 (이름 포함, 대소문자 무시)
     *
     * @param userId 사용자 ID
     * @param input  검색어
     * @return 검색 결과 DTO 리스트
     */
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