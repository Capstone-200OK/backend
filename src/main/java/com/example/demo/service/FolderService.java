package com.example.demo.service;

import com.example.demo.dto.fileDTO.FilePythonRequestDTO;
import com.example.demo.dto.folderDTO.FolderPythonRequestDTO;
import com.example.demo.dto.folderDTO.FolderRequestDTO;
import com.example.demo.dto.folderDTO.FolderResult;
import com.example.demo.dto.folderDTO.FolderSelectableDTO;
import com.example.demo.entity.*;
import com.example.demo.repository.FileRepository;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final FolderAccessService folderAccessService;

    @Transactional
    public Optional<Folder> findFolderByName(FolderRequestDTO folderRequestDTO) {
        User user = userRepository.findById(folderRequestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Folder parent = null;
        if (folderRequestDTO.getParentFolderId() != null) {
            parent = folderRepository.findById(folderRequestDTO.getParentFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
        }

        FolderType type = folderRequestDTO.getFolderType() != null ? folderRequestDTO.getFolderType() : FolderType.PERSONAL;

        if (type == FolderType.PERSONAL) {
            if (parent == null) {
                return folderRepository.findByNameAndParentFolderIsNullAndUser(folderRequestDTO.getName(), user);
            } else {
                return folderRepository.findByNameAndParentFolderAndUser(folderRequestDTO.getName(), parent, user);
            }
        } else { // CLOUD
            if (parent == null) {
                return folderAccessService.findAccessibleRootCloudFolderByName(user, folderRequestDTO.getName());
            } else {
                return folderAccessService.findAccessibleSubCloudFolderByName(user, parent, folderRequestDTO.getName());
            }
        }
    }

    @Transactional
    public Folder addFolder(FolderRequestDTO folderRequestDTO) {
        User user = userRepository.findById(folderRequestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Folder parent = null;
        if (folderRequestDTO.getParentFolderId() != null) {
            parent = folderRepository.findById(folderRequestDTO.getParentFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
        }

        FolderType type = folderRequestDTO.getFolderType() != null ? folderRequestDTO.getFolderType() : FolderType.PERSONAL;

        if (type == FolderType.CLOUD && parent != null) {
            // Cloud일 경우 parentFolder에 대한 write 권한이 있어야 함
            if (!folderAccessService.canWrite(user, parent)) {
                throw new RuntimeException("You do not have write permission for the parent cloud folder.");
            }
        }

        String resolvedName = resolveDuplicateFolderName(user.getId(), parent == null ? null : parent.getId(), folderRequestDTO.getName());

        Folder folder = Folder.builder()
                .name(resolvedName)
                .user(user)
                .parentFolder(parent)
                .folderType(type)
                .isDeleted(false)
                .build();

        Folder saved = folderRepository.save(folder);

        // 생성자에게 full 권한 부여
        folderAccessService.grantAccess(user.getId(), saved.getId(), 7);

        // 부모 폴더 권한 상속 (Cloud 폴더의 경우만 의미 있음)
        if (parent != null) {
            List<FolderAccess> parentAccesses = folderAccessService.getAllAccessByFolder(parent.getId());
            for (FolderAccess access : parentAccesses) {
                if (!access.getUser().getId().equals(user.getId())) {
                    folderAccessService.grantAccess(access.getUser().getId(), saved.getId(), access.getChmod());
                }
            }
        }

        return saved;
    }

    @Transactional
    public Folder findOrCreateFolder(FolderRequestDTO folderRequestDTO) {
        return findFolderByName(folderRequestDTO)
                .orElseGet(() -> addFolder(folderRequestDTO));
    }

    @Transactional
    public FolderResult findOrCreateFolderWithFlag(FolderRequestDTO folderRequestDTO) {
        Optional<Folder> found = findFolderByName(folderRequestDTO);
        if (found.isPresent()) {
            return new FolderResult(found.get(), false);
        } else {
            Folder created = addFolder(folderRequestDTO);
            return new FolderResult(created, true);
        }
    }

    @Transactional
    public FolderPythonRequestDTO getFolderHierarchy(Long folderId, Long userId) {
        Folder folder = folderRepository.findByIdAndIsDeletedFalse(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (folder.getFolderType() == FolderType.PERSONAL) {
            if (!isPersonalRoot(folder)) { // ✅ 루트가 아닌 경우만 검사
                if (!folder.getUser().getId().equals(user.getId())) {
                    throw new RuntimeException("No access to this personal folder.");
                }
            }
            // 루트 personal 폴더는 모든 user 허용
        } else {
            if (!isCloudRoot(folder)) {
                if (!folderAccessService.canAccess(user, folder)) {
                    throw new RuntimeException("No access to this cloud folder.");
                }
            }
        }

        return buildFolderDTO(folder, user);
    }


    private boolean isPersonalRoot(Folder folder) {
        return folder.getParentFolder() == null && folder.getFolderType() == FolderType.PERSONAL;
    }

    private boolean isCloudRoot(Folder folder) {
        return folder.getParentFolder() == null && folder.getFolderType() == FolderType.CLOUD;
    }

    @Transactional
    public Long getFolderIdByPath(Long userId, String path) {
        String[] names = path.split("/");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Folder current = null;
        FolderType currentType = FolderType.PERSONAL; // Default

        for (String name : names) {
            if (current == null) {
                Optional<Folder> personal = folderRepository.findByNameAndParentFolderIsNullAndUser(name, user);
                Optional<Folder> cloud = folderAccessService.findAccessibleRootCloudFolderByName(user, name);

                if (personal.isPresent()) {
                    current = personal.get();
                    currentType = FolderType.PERSONAL;
                } else if (cloud.isPresent()) {
                    current = cloud.get();
                    currentType = FolderType.CLOUD;
                } else {
                    throw new RuntimeException("Root folder not found: " + name);
                }
            } else {
                if (currentType == FolderType.PERSONAL) {
                    current = folderRepository.findByNameAndParentFolderAndUser(name, current, user)
                            .orElseThrow(() -> new RuntimeException("Subfolder not found: " + name));
                } else {
                    current = folderAccessService.findAccessibleSubCloudFolderByName(user, current, name)
                            .orElseThrow(() -> new RuntimeException("Subfolder not found in cloud: " + name));
                }
            }
        }

        return current.getId();
    }

    private String resolveDuplicateFolderName(Long userId, Long parentFolderId, String baseName) {
        String newName = baseName;
        int counter = 1;

        if (parentFolderId == null) {
            while (folderRepository.existsByUserIdAndParentFolderIsNullAndNameAndIsDeletedFalse(userId, newName)) {
                newName = baseName + "(" + counter++ + ")";
            }
        } else {
            while (folderRepository.existsByParentFolderIdAndNameAndIsDeletedFalse(parentFolderId, newName)) {
                newName = baseName + "(" + counter++ + ")";
            }
        }

        return newName;
    }

    private FolderPythonRequestDTO buildFolderDTO(Folder folder, User user) {
        FolderPythonRequestDTO dto = new FolderPythonRequestDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());
        dto.setIsDeleted(folder.getIsDeleted());

        List<File> files = fileRepository.findByFolderIdAndIsDeletedFalse(folder.getId());
        List<FilePythonRequestDTO> fileDTOList = files.stream()
                .filter(file -> {
                    if (folder.getFolderType() == FolderType.PERSONAL) {
                        // ✅ 개인 폴더는 본인 파일만
                        return file.getUser().getId().equals(user.getId());
                    } else {
                        // ✅ 클라우드 폴더는 Read 권한이 있으면 모든 파일 볼 수 있음
                        return folderAccessService.canRead(user, folder);
                    }
                })
                .map(f -> new FilePythonRequestDTO(
                        f.getId(), f.getName(), f.getFilePath(), f.getFileType(),
                        f.getSize(), f.getIsImportant(), f.getIsDeleted(), f.getCreatedAt(), f.getFileUrl(), f.getFileThumbUrl()
                ))
                .toList();
        dto.setFiles(fileDTOList);


        // 하위 폴더 권한 필터링
        List<FolderPythonRequestDTO> subFolderDTOs = folder.getSubFolders().stream()
                .filter(child -> !child.getIsDeleted())
                .filter(child -> {
                    if (child.getFolderType() == FolderType.PERSONAL) {
                        return child.getUser().getId().equals(user.getId());
                    } else {
                        return folderAccessService.canAccess(user, child);
                    }
                })
                .map(child -> buildFolderDTO(child, user)) // 재귀 호출 시에도 user 전달
                .toList();

        dto.setSubFolders(subFolderDTOs);
        return dto;
    }

    @Transactional
    public List<FolderSelectableDTO> findSelectableFolders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Folder> allFolders = folderRepository.findAllByIsDeletedFalse();

        return allFolders.stream()
                .filter(folder -> {
                    if (folder.getFolderType() == FolderType.PERSONAL) {
                        return folder.getUser() != null && folder.getUser().getId().equals(user.getId());
                    } else { // CLOUD
                        return folderAccessService.hasFullPermission(user, folder);
                    }
                })
                .map(folder -> new FolderSelectableDTO(
                        folder.getId(),
                        folder.getName(),
                        folder.getParentFolder() != null ? folder.getParentFolder().getId() : null,
                        folder.getFolderType()
                ))
                .toList();
    }


    public Folder getFolderById(Long id) {
        return folderRepository.findById(id).orElseThrow(() -> new RuntimeException("Folder not found"));
    }

    public String buildFullPath(Folder folder) {
        StringBuilder pathBuilder = new StringBuilder();
        buildPathRecursive(folder, pathBuilder);
        return pathBuilder.toString();
    }
    private void buildPathRecursive(Folder folder, StringBuilder pathBuilder) {
        if (folder.getParentFolder() != null) {
            buildPathRecursive(folder.getParentFolder(), pathBuilder);
            pathBuilder.append("/");  // 구분자 추가
        }
        pathBuilder.append(folder.getName());
    }
}
