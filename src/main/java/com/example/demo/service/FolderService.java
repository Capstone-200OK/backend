package com.example.demo.service;

import com.example.demo.dto.fileDTO.FilePythonRequestDTO;
import com.example.demo.dto.folderDTO.*;
import com.example.demo.entity.*;
import com.example.demo.repository.FileRepository;
import com.example.demo.repository.FolderAccessRepository;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final FolderAccessService folderAccessService;
    private final FolderAccessRepository folderAccessRepository;

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
        } else {
            parent = folderRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Root folder (id=1) not found"));
        }

        FolderType type = parent.getFolderType() != null ? parent.getFolderType() : FolderType.PERSONAL;

        if (type == FolderType.CLOUD) {
            // Cloud일 경우 parentFolder에 대한 write 권한이 있어야 함
            if (parent.getId() != 2L && !folderAccessService.canWrite(user, parent)) {
                throw new RuntimeException("You do not have write permission for the parent cloud folder.");
            }
        }

        String resolvedName = resolveDuplicateFolderName(user.getId(), parent.getId(), folderRequestDTO.getName());

        Folder folder = Folder.builder()
                .name(resolvedName)
                .user(user)
                .parentFolder(parent)
                .folderType(type)
                .isDeleted(false)
                .build();

        Folder saved = folderRepository.save(folder);

        // 생성자에게 full 권한 부여
        if (folder.getFolderType() == FolderType.CLOUD) {
            folderAccessService.grantAccess(user.getId(), saved.getId(), 7);
        }
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
    public List<FolderPythonRequestDTO> getAccessibleCloudRoots(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. 유저가 접근 가능한 모든 클라우드 폴더 조회
        List<Folder> allAccessible = folderAccessService.getAccessibleCloudFolders(user).stream()
                .filter(f -> f.getFolderType() == FolderType.CLOUD && !f.getIsDeleted())
                .toList();

        // 2. 그 중에서 부모 폴더에 접근 권한이 없는 경우만 필터링 → 루트처럼 보이게 함
        List<Folder> topLevelVisible = allAccessible.stream()
                .filter(folder -> {
                    Folder parent = folder.getParentFolder();
                    return parent == null || !folderAccessService.canAccess(user, parent);
                })
                .toList();

        return topLevelVisible.stream()
                .map(folder -> buildFolderDTO(folder, user))
                .toList();
    }


    @Transactional
    public List<FolderSelectableDTO> findSelectableFolders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Folder> allFolders = folderRepository.findAllByIsDeletedFalse();

        return allFolders.stream()
                .filter(folder -> {
                    if (folder.getId() == 1L|| folder.getId() == 2L) return true; // ✅ Root 폴더는 모든 유저에게 허용
                    if (folder.getFolderType() == FolderType.PERSONAL) {
                        return folder.getUser() != null && folder.getUser().getId().equals(user.getId());
                    } else {
                        System.out.println(user.getId() + ", " +folder.getId());
                        boolean result = folderAccessService.hasFullPermission(user, folder);
                        System.out.println("result" + result);
                        return result;
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

    public List<FolderPathResponseDTO> getFolderPath(Long folderId) {
        Folder folder = folderRepository.findByIdAndIsDeletedFalse(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        List<FolderPathResponseDTO> path = new ArrayList<>();

        // 현재 폴더부터 상위 폴더까지 역순으로 수집
        while (folder != null) {
            path.add(FolderPathResponseDTO.builder()
                    .folderId(folder.getId())
                    .folderName(folder.getName())
                    .build());

            folder = folder.getParentFolder();
        }

        // Root → ... → 대상 순서로 정렬
        Collections.reverse(path);

        return path;
    }

    public List<FolderSearchResponseDTO> searchFolder(Long userId, String input) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<FolderSearchResponseDTO> folders = new ArrayList<>();
        Set<Long> addedFolderIds = new HashSet<>();

        // 🔹 내 폴더 검색
        List<Folder> folderList =
                folderRepository.findAllByNameContainingIgnoreCaseAndIsDeletedFalseAndUser(input, user);

        for (Folder folder : folderList) {
            addedFolderIds.add(folder.getId());
        }

        // 🔹 다른 사용자의 CLOUD 폴더 중 접근 가능한 것만
        List<Folder> externalFolders =
                folderRepository.searchExternalCloudFolders(input, user);

        for (Folder folder : externalFolders) {
            List<FolderAccess> accesses = folderAccessRepository.findAllByFolder(folder);
            for (FolderAccess access : accesses) {
                if (access.getUser().equals(user) && !addedFolderIds.contains(folder.getId())) {
                    folderList.add(folder);
                    addedFolderIds.add(folder.getId());
                    break; // 한 번 추가되면 더 이상 확인할 필요 없음
                }
            }
        }

        // 🔹 DTO 변환
        for (Folder folder : folderList) {
            if (folder.getParentFolder() == null) continue; // 예외 처리
            folders.add(FolderSearchResponseDTO.builder()
                    .folderId(folder.getId())
                    .folderName(folder.getName())
                    .parentFolderName(folder.getParentFolder().getName())
                    .folderType(folder.getFolderType())
                    .build());
        }

        return folders;
    }
}
