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

@Service
@RequiredArgsConstructor
public class FolderService {

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final FolderAccessService folderAccessService;
    private final FolderAccessRepository folderAccessRepository;

    /**
     * 사용자의 폴더 중 이름과 부모 폴더로 찾기
     *
     * @param folderRequestDTO 폴더 요청 정보
     * @return 찾은 폴더 Optional
     */
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

    /**
     * 폴더 생성 (권한 및 중복 이름 체크 포함)
     *
     * @param folderRequestDTO 폴더 요청 정보
     * @return 생성된 폴더
     */
    @Transactional
    public Folder addFolder(FolderRequestDTO folderRequestDTO) {
        User user = userRepository.findById(folderRequestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Folder parent;
        if (folderRequestDTO.getParentFolderId() != null) {
            parent = folderRepository.findById(folderRequestDTO.getParentFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
        } else {
            parent = folderRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Root folder (id=1) not found"));
        }

        FolderType type = parent.getFolderType() != null ? parent.getFolderType() : FolderType.PERSONAL;

        if (type == FolderType.CLOUD) {
            // 클라우드 폴더는 부모 폴더에 쓰기 권한이 있어야 함
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

        // 클라우드 폴더는 권한 부여 필요
        if (folder.getFolderType() == FolderType.CLOUD) {
            folderAccessService.grantAccess(user.getId(), saved.getId(), 7);
        }

        // 부모 폴더 권한 상속
        List<FolderAccess> parentAccesses = folderAccessService.getAllAccessByFolder(parent.getId());
        for (FolderAccess access : parentAccesses) {
            if (!access.getUser().getId().equals(user.getId())) {
                folderAccessService.grantAccess(access.getUser().getId(), saved.getId(), access.getChmod());
            }
        }

        return saved;
    }

    /**
     * 폴더가 존재하지 않으면 새로 생성
     *
     * @param folderRequestDTO 폴더 요청 정보
     * @return 존재하는 또는 생성된 폴더
     */
    @Transactional
    public Folder findOrCreateFolder(FolderRequestDTO folderRequestDTO) {
        return findFolderByName(folderRequestDTO)
                .orElseGet(() -> addFolder(folderRequestDTO));
    }

    /**
     * 폴더를 찾거나 생성하며, 새로 생성했는지 여부도 반환
     *
     * @param folderRequestDTO 폴더 요청 정보
     * @return FolderResult (폴더, 생성 여부)
     */
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

    /**
     * 해당 폴더를 기준으로 전체 하위 구조를 반환 (Python 통신용)
     *
     * @param folderId 폴더 ID
     * @param userId   사용자 ID
     * @return FolderPythonRequestDTO
     */
    @Transactional
    public FolderPythonRequestDTO getFolderHierarchy(Long folderId, Long userId) {
        Folder folder = folderRepository.findByIdAndIsDeletedFalse(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 권한 검사
        if (folder.getFolderType() == FolderType.PERSONAL) {
            if (!isPersonalRoot(folder) && !folder.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("No access to this personal folder.");
            }
        } else {
            if (!isCloudRoot(folder) && !folderAccessService.canAccess(user, folder)) {
                throw new RuntimeException("No access to this cloud folder.");
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

    /**
     * 사용자와 경로로 폴더 ID 조회
     *
     * @param userId 사용자 ID
     * @param path   경로 문자열
     * @return 폴더 ID
     */
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

    /**
     * 중복 폴더명을 방지하기 위한 이름 생성
     *
     * @param userId         사용자 ID
     * @param parentFolderId 부모 폴더 ID (null 가능)
     * @param baseName       생성할 폴더의 기본 이름
     * @return 중복되지 않는 폴더 이름
     */
    private String resolveDuplicateFolderName(Long userId, Long parentFolderId, String baseName) {
        String newName = baseName;
        int counter = 1;

        if (parentFolderId == null) {
            while (folderRepository.existsByUserIdAndParentFolderIsNullAndNameAndIsDeletedFalse(userId, newName)) {
                newName = baseName + "(" + counter++ + ")";
            }
        } else {
            while (folderRepository.existsByUserIdAndParentFolderIdAndNameAndIsDeletedFalse(userId, parentFolderId, newName)) {
                newName = baseName + "(" + counter++ + ")";
            }
        }

        return newName;
    }

    /**
     * 폴더 및 그 내부 파일, 하위 폴더를 재귀적으로 DTO로 변환
     *
     * @param folder 대상 폴더
     * @param user   접근 유저
     * @return 변환된 FolderPythonRequestDTO
     */
    private FolderPythonRequestDTO buildFolderDTO(Folder folder, User user) {
        FolderPythonRequestDTO dto = new FolderPythonRequestDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());
        dto.setIsDeleted(folder.getIsDeleted());

        // 파일 목록 구성
        List<File> files = fileRepository.findByFolderIdAndIsDeletedFalse(folder.getId());
        List<FilePythonRequestDTO> fileDTOList = files.stream()
                .filter(file -> {
                    if (folder.getFolderType() == FolderType.PERSONAL) {
                        return file.getUser().getId().equals(user.getId());
                    } else {
                        return folderAccessService.canRead(user, folder);
                    }
                })
                .map(f -> new FilePythonRequestDTO(
                        f.getId(), f.getName(), f.getFilePath(), f.getFileType(),
                        f.getSize(), f.getIsImportant(), f.getIsDeleted(), f.getCreatedAt(), f.getFileUrl(), f.getFileThumbUrl()
                ))
                .toList();
        dto.setFiles(fileDTOList);

        // 하위 폴더 필터링 및 재귀 변환
        List<FolderPythonRequestDTO> subFolderDTOs = folder.getSubFolders().stream()
                .filter(child -> !child.getIsDeleted())
                .filter(child -> {
                    if (child.getFolderType() == FolderType.PERSONAL) {
                        return child.getUser().getId().equals(user.getId());
                    } else {
                        return folderAccessService.canAccess(user, child);
                    }
                })
                .map(child -> buildFolderDTO(child, user))
                .toList();

        dto.setSubFolders(subFolderDTOs);
        return dto;
    }

    /**
     * 사용자가 접근 가능한 클라우드 루트 폴더 목록 반환
     *
     * @param userId 사용자 ID
     * @return 클라우드 루트 폴더 목록
     */
    @Transactional
    public List<FolderPythonRequestDTO> getAccessibleCloudRoots(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Folder> allAccessible = folderAccessService.getAccessibleCloudFolders(user).stream()
                .filter(f -> f.getFolderType() == FolderType.CLOUD && !f.getIsDeleted())
                .toList();

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


    /**
     * 사용자가 이동 가능한 폴더 리스트 반환
     *
     * @param userId 사용자 ID
     * @return 이동 가능 폴더 목록
     */
    @Transactional
    public List<FolderSelectableDTO> findSelectableFolders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Folder> allFolders = folderRepository.findAllByIsDeletedFalse();

        return allFolders.stream()
                .filter(folder -> {
                    if (folder.getId() == 1L || folder.getId() == 2L) return true;
                    if (folder.getFolderType() == FolderType.PERSONAL) {
                        return folder.getUser() != null && folder.getUser().getId().equals(user.getId());
                    } else {
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


    /**
     * 폴더 ID로 폴더 조회
     *
     * @param id 폴더 ID
     * @return 폴더 엔티티
     */
    public Folder getFolderById(Long id) {
        return folderRepository.findById(id).orElseThrow(() -> new RuntimeException("Folder not found"));
    }

    /**
     * 폴더의 전체 경로를 문자열로 반환
     *
     * @param folder 대상 폴더
     * @return 전체 경로 문자열
     */
    public String buildFullPath(Folder folder) {
        StringBuilder pathBuilder = new StringBuilder();
        buildPathRecursive(folder, pathBuilder);
        return pathBuilder.toString();
    }

    private void buildPathRecursive(Folder folder, StringBuilder pathBuilder) {
        if (folder.getParentFolder() != null) {
            buildPathRecursive(folder.getParentFolder(), pathBuilder);
            pathBuilder.append("/");
        }
        pathBuilder.append(folder.getName());
    }
    public String buildAbsolutePath(Long folderId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        StringBuilder pathBuilder = new StringBuilder();
        buildPathRecursive(folder, pathBuilder);
        return pathBuilder.toString();
    }

    /**
     * 폴더 ID로부터 상위 폴더 경로를 구해 반환
     *
     * @param folderId 폴더 ID
     * @param userId   사용자 ID (null 가능)
     * @return 상위 경로 리스트
     */
    public List<FolderPathResponseDTO> getFolderPath(Long folderId, Long userId) {
        Folder folder = folderRepository.findByIdAndIsDeletedFalse(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        List<FolderPathResponseDTO> path = new ArrayList<>();

        while (folder != null) {
            if (userId != null && !folderAccessRepository.existsByUserIdAndFolderId(userId, folder.getId()))
                break;

            path.add(FolderPathResponseDTO.builder()
                    .folderId(folder.getId())
                    .folderName(folder.getName())
                    .build());

            folder = folder.getParentFolder();
        }

        Collections.reverse(path);
        return path;
    }

    /**
     * 사용자 입력으로 폴더 이름 검색 (내 폴더 + 접근 가능한 외부 클라우드)
     *
     * @param userId 사용자 ID
     * @param input  검색어
     * @return 검색된 폴더 리스트
     */
    public List<FolderSearchResponseDTO> searchFolder(Long userId, String input) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<FolderSearchResponseDTO> folders = new ArrayList<>();
        Set<Long> addedFolderIds = new HashSet<>();

        List<Folder> folderList = folderRepository.findAllByNameContainingIgnoreCaseAndIsDeletedFalseAndUser(input, user);
        for (Folder folder : folderList) {
            addedFolderIds.add(folder.getId());
        }

        List<Folder> externalFolders = folderRepository.searchExternalCloudFolders(input, user);
        for (Folder folder : externalFolders) {
            List<FolderAccess> accesses = folderAccessRepository.findAllByFolder(folder);
            for (FolderAccess access : accesses) {
                if (access.getUser().equals(user) && !addedFolderIds.contains(folder.getId())) {
                    folderList.add(folder);
                    addedFolderIds.add(folder.getId());
                    break;
                }
            }
        }

        for (Folder folder : folderList) {
            if (folder.getParentFolder() == null) continue;
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
